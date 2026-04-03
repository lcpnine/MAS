package tileworld.agent;

import sim.util.Int2D;
import tileworld.Parameters;
import tileworld.environment.TWDirection;
import tileworld.environment.TWEnvironment;

/**                                                                                                                                         
 * ExplorerAgent — Systematic zone coverage - nik
 *
 * Specialization: Zone-based exploration (6 strips), hotspot bias, SWAP broadcasts when zone done
 */
public class ExplorerAgent extends SmartTWAgent {

    private boolean zoneFullyExplored = false;
    private boolean swapBroadcast = false;
    private final int zoneStartX;
    private final int zoneEndX;

    public ExplorerAgent(String name, int xpos, int ypos, TWEnvironment env, double fuelLevel, int agentIndex) {
        super(name, xpos, ypos, env, fuelLevel, agentIndex);
        int zoneWidth = env.getxDimension()/6;
        this.zoneStartX = agentIndex*zoneWidth;
        this.zoneEndX = (agentIndex==5) ? env.getxDimension()-1 : zoneStartX+zoneWidth-1;
    }

    @Override
    protected TWThought think() {
        // Priority 0 - get to the densest hotspot only if its whithin zone and distant
        if(!hotspots.isEmpty()){
            HotspotEntry best = getBestHotspotEntry();
            if(best !=null && isInMyZone(best.position.x, best.position.y)){
                int dist = Math.abs(getX()-best.position.x)+Math.abs(getY()-best.position.y);
                if(dist>15){
                    TWDirection dir = navigateTo(best.position.x,best.position.y, "explore_hotspot");
                    if(dir != null){
                        return new TWThought(TWAction.MOVE, dir);
                    }
                }
            }
        }
        // Priority 1 - Move to least recent visited cell within the zone
        int radius = getSmartMemory().isLargeGrid() ? 20:10;
        Int2D target = getSmartMemory().getLeastVisitedTarget(radius);
        if (target != null) {
            int distanceToTarget = Math.abs(getX() - target.x)+Math.abs(getY()-target.y);
            if (distanceToTarget>2){
                TWDirection dir = navigateTo(target.x, target.y, "explore");
                if(dir != null){
                        return new TWThought(TWAction.MOVE, dir);
                    }
            }
        }
        // Priority 2 - Check fully if zone is 80% covered
        if(!zoneFullyExplored && isZoneDone()){
            zoneFullyExplored = true;
        }
        // Priority 3 - Set base agent which will handle all the fuel, pickup, and delivery
        return super.think();
    }

    @Override
    public void communicate() {
        super.communicate();
        if(zoneFullyExplored && !swapBroadcast){
            getEnvironment().receiveMessage(new Message(getName(), "", "SWAP:"+getName()+",ZONE_DONE"));
            swapBroadcast = true;
        }
    }

    // --->>> Helpers :)
    private boolean isInMyZone(int x, int y){
        boolean conditionsIn = x>=zoneStartX && x<=zoneEndX && y>=0 && y<getSmartMemory().getYDim();
        return conditionsIn;
    }
    private boolean isZoneDone(){
        double threshold = getEnvironment().schedule.getTime() - Parameters.lifeTime;
        int yDim = getSmartMemory().getYDim();
        int visited=0;
        int total=0;
        for (int x=zoneStartX; x<=zoneEndX; x++){
            for (int y=0; y<yDim; y++){
                total++;
                if(getSmartMemory().getObservationTime(x, y)>=threshold){
                    visited++;
                }
            }
        }
        boolean conditionsDone = total>0 && (double) visited/total >=0.8;
        return conditionsDone;
    }
    private HotspotEntry getBestHotspotEntry(){
        HotspotEntry best = null;
        double bestDensity = 0;
        for(HotspotEntry entry: hotspots){
            if(entry.density > bestDensity){
                bestDensity = entry.density;
                best = entry;
            }
        }
        return best;
    }
}
