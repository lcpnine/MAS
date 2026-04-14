#!/usr/bin/env bash
# Shared config-switching helpers for TileWorld build scripts.
# Source this file; do not execute it directly.
#
# Requires ROOT_DIR to be set by the sourcing script.

PARAMS="${ROOT_DIR}/Tileworld/src/tileworld/Parameters.java"

set_config1() {
  perl -0777 -i -pe '
    s/(xDimension\s*=\s*)\d+/${1}50/g;
    s/(yDimension\s*=\s*)\d+/${1}50/g;
    s/(tileMean\s*=\s*)[\d.]+/${1}0.2/g;
    s/(holeMean\s*=\s*)[\d.]+/${1}0.2/g;
    s/(obstacleMean\s*=\s*)[\d.]+/${1}0.2/g;
    s/(tileDev\s*=\s*)[\d.f]+/${1}0.05f/g;
    s/(holeDev\s*=\s*)[\d.f]+/${1}0.05f/g;
    s/(obstacleDev\s*=\s*)[\d.f]+/${1}0.05f/g;
    s/(lifeTime\s*=\s*)\d+/${1}100/g;
  ' "$PARAMS"
}

set_config2() {
  perl -0777 -i -pe '
    s/(xDimension\s*=\s*)\d+/${1}80/g;
    s/(yDimension\s*=\s*)\d+/${1}80/g;
    s/(tileMean\s*=\s*)[\d.]+/${1}2.0/g;
    s/(holeMean\s*=\s*)[\d.]+/${1}2.0/g;
    s/(obstacleMean\s*=\s*)[\d.]+/${1}2.0/g;
    s/(tileDev\s*=\s*)[\d.f]+/${1}0.5/g;
    s/(holeDev\s*=\s*)[\d.f]+/${1}0.5/g;
    s/(obstacleDev\s*=\s*)[\d.f]+/${1}0.5/g;
    s/(lifeTime\s*=\s*)\d+/${1}30/g;
  ' "$PARAMS"
}


set_config3() {
  perl -0777 -i -pe '
    s/(xDimension\s*=\s*)\d+/${1}120/g;
    s/(yDimension\s*=\s*)\d+/${1}120/g;
    s/(tileMean\s*=\s*)[\d.]+/${1}5.0/g;
    s/(holeMean\s*=\s*)[\d.]+/${1}5.0/g;
    s/(obstacleMean\s*=\s*)[\d.]+/${1}5.0/g;
    s/(tileDev\s*=\s*)[\d.f]+/${1}0.5/g;
    s/(holeDev\s*=\s*)[\d.f]+/${1}0.5/g;
    s/(obstacleDev\s*=\s*)[\d.f]+/${1}0.5/g;
    s/(lifeTime\s*=\s*)\d+/${1}50/g;
  ' "$PARAMS"
}