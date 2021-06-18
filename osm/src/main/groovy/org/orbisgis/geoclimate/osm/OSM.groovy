package org.orbisgis.geoclimate.osm

import org.orbisgis.orbisdata.processmanager.process.GroovyProcessFactory
import org.slf4j.LoggerFactory

/**
 * Main class to access to the OSM processes
 *
 */
abstract class OSM  extends GroovyProcessFactory {

    public static def logger = LoggerFactory.getLogger(OSM.class)

    public static WorkflowOSM = new WorkflowOSM()
    public static InputDataLoading = new InputDataLoading()
    public static InputDataFormatting = new InputDataFormatting()


    static def uuid = { UUID.randomUUID().toString().replaceAll("-", "_") }
    static def getUuid() { UUID.randomUUID().toString().replaceAll("-", "_") }
    static def info = { obj -> logger.info(obj.toString()) }
    static def warn = { obj -> logger.warn(obj.toString()) }
    static def error = { obj -> logger.error(obj.toString()) }
    static debug = { obj -> logger.debug(obj.toString()) }

    /**
     * Utility method to generate a name
     * @param prefixName
     * @param baseName
     * @return
     */
    static def getOutputTableName(prefixName, baseName){
        if (!prefixName){
            return baseName
        }
        else{
            return prefixName + "_" + baseName
        }
    }

}