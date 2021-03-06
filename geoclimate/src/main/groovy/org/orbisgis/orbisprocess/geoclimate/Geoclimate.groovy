package org.orbisgis.orbisprocess.geoclimate

import org.orbisgis.orbisprocess.geoclimate.geoindicators.*
import org.orbisgis.orbisprocess.geoclimate.processingchain.*
import org.orbisgis.orbisprocess.geoclimate.osm.*
import org.orbisgis.orbisprocess.geoclimate.bdtopo_v2.*

/**
 * Root access point to the Geoindicators processes.
 */
class Geoclimate {

    public static def GeoIndicatorsChain  = new GeoIndicatorsChain()
    public static def DataUtils  = new DataUtils()
    public static def BuildingIndicators = new BuildingIndicators()
    public static def RsuIndicators = new RsuIndicators()
    public static def BlockIndicators = new BlockIndicators()
    public static def GenericIndicators = new GenericIndicators()
    public static def SpatialUnits = new SpatialUnits()
    public static def TypologyClassification = new TypologyClassification()
    public static def OSM = new OSM()
    public static def BDTOPO_V2 = new BDTopo_V2()

    /**
     * Set the logger for all the processes.
     *
     * @param logger Logger to use in the processes.
     */
    static void setLogger(def logger){
        OSM_Utils.logger = logger
        BDTopo_V2_Utils.logger = logger
        ProcessingChain.logger = logger
        Geoindicators.logger = logger
    }
}