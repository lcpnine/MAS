$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

$patchFiles = @(
    "Tileworld\src\tileworld\environment\TWEnvironment.java",
    "Tileworld\src\tileworld\environment\TWObjectCreator.java",
    "Tileworld\src\tileworld\TileworldMain.java"
)

$javacPath = (Get-Command javac).Source
$jdkBin    = Split-Path $javacPath -Parent
$javaPath  = Join-Path $jdkBin "java.exe"
if (-not (Test-Path $javaPath)) {
    $javaPath = (Get-Command java).Source
}

$logDir = Join-Path $root "test-logs"
New-Item -ItemType Directory -Path $logDir -Force | Out-Null

function Read-TextRaw([string]$path) {
    return [System.IO.File]::ReadAllText($path)
}

function Write-TextUtf8NoBom([string]$path, [string]$content) {
    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($path, $content, $utf8NoBom)
}

function Set-Config1 {
    foreach ($f in $patchFiles) {
        $text = Read-TextRaw $f
        $text = $text -replace "import\s+tileworld\.Parameters2;", "import tileworld.Parameters;"
        $text = $text -replace "\bParameters2\.", "Parameters."
        Write-TextUtf8NoBom $f $text
    }
}

function Set-Config2 {
    foreach ($f in $patchFiles) {
        $text = Read-TextRaw $f
        $text = $text -replace "import\s+tileworld\.Parameters;", "import tileworld.Parameters2;"
        $text = $text -replace "\bParameters\.", "Parameters2."
        Write-TextUtf8NoBom $f $text
    }
}

function Build-And-Run([string]$label) {
    Write-Host "=== $label ==="

    $files = Get-ChildItem -Path "Tileworld\src" -Recurse -Filter "*.java" | Select-Object -ExpandProperty FullName
    & $javacPath -cp "lib\MASON_14.jar" -d "Tileworld\bin" $files
    if ($LASTEXITCODE -ne 0) { throw "Compilation failed for $label" }

    $logFile = Join-Path $logDir ("{0}-{1}.log" -f $label, (Get-Date -Format "yyyyMMdd-HHmmss"))
    & $javaPath -cp "Tileworld\bin;lib\MASON_14.jar" tileworld.TileworldMain 2>&1 |
        Tee-Object -FilePath $logFile
    if ($LASTEXITCODE -ne 0) { throw "Run failed for $label" }

    Write-Host "--- Summary ($label) ---"
    Select-String -Path $logFile -Pattern "Seed:|The final reward is:|The average reward is:" |
        ForEach-Object { $_.Line }
    Write-Host "Log: $logFile"
}

try {
    foreach ($f in $patchFiles) { Copy-Item $f "$f.bak" -Force }

    Set-Config1
    Build-And-Run "config1"

    Set-Config2
    Build-And-Run "config2"
}
finally {
    foreach ($f in $patchFiles) {
        if (Test-Path "$f.bak") { Move-Item "$f.bak" $f -Force }
    }
}
