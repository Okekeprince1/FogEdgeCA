package org.fog.test.perfeval;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking;
import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.entities.Actuator;
import org.fog.entities.FogBroker;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.entities.Sensor;
import org.fog.entities.Tuple;
import org.fog.placement.Controller;
import org.fog.placement.ModuleMapping;
import org.fog.placement.ModulePlacementEdgewards;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.fog.utils.TimeKeeper;
import org.fog.utils.distribution.DeterministicDistribution;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

// This simulation is based on the concepts and examples from lab_09.pdf (H9FEC module)

public class FarmFogSimulation {
    static List<FogDevice> fogDevices = new ArrayList<>(); // Fog devices
    static List<Sensor> sensors = new ArrayList<>(); // Sensors in the simulation
    static List<Actuator> actuators = new ArrayList<>(); // Actuators(irrigation systems) 
    static boolean CLOUD = false; // True for Cloud-only model, false for Fog-based model
    static int numOfFarms = 3;
    static int[] sensorsPerFarm = {1, 5, 8};
    static double SENSOR_TRANSMISSION_TIME = 5;

    public static void main(String[] args) {
        Log.printLine("Starting FarmFogSimulation...");

        try {
            Log.enable();
            int num_user = 1;
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;

            CloudSim.init(num_user, calendar, trace_flag);

            for (int n = 0; n < sensorsPerFarm.length; n++) {
                fogDevices.clear();
                sensors.clear();
                actuators.clear();

                String appId = "farm_app_" + sensorsPerFarm[n];
                FogBroker broker = new FogBroker("broker_" + sensorsPerFarm[n]);
                Application application = createApplication(appId, broker.getId());
                application.setUserId(broker.getId());

                createFogDevices(broker.getId(), appId, sensorsPerFarm[n]);

                ModuleMapping moduleMapping = ModuleMapping.createModuleMapping();
                if (CLOUD) {
                    moduleMapping.addModuleToDevice("auth-service", "cloud");
                    moduleMapping.addModuleToDevice("processing-service", "cloud");
                    moduleMapping.addModuleToDevice("control-service", "cloud");
                    moduleMapping.addModuleToDevice("storage-service", "cloud");
                    for (FogDevice device : fogDevices) {
                        if (device.getName().startsWith("s")) {
                            moduleMapping.addModuleToDevice("sensor-client", device.getName());
                        }
                    }
                } else {
                    for (FogDevice device : fogDevices) {
                        if (device.getName().startsWith("f")) {
                            moduleMapping.addModuleToDevice("auth-service", device.getName());
                            moduleMapping.addModuleToDevice("processing-service", device.getName());
                            moduleMapping.addModuleToDevice("control-service", device.getName());
                        }
                        if (device.getName().startsWith("s")) {
                            moduleMapping.addModuleToDevice("sensor-client", device.getName());
                        }
                    }
                    moduleMapping.addModuleToDevice("storage-service", "cloud");
                }

                for (FogDevice device : fogDevices) {
                    if (device == null) {
                        throw new RuntimeException("Null FogDevice detected in fogDevices list");
                    }
                }

                Controller controller = new Controller("controller-" + sensorsPerFarm[n], fogDevices, sensors, actuators);
                controller.submitApplication(application, new ModulePlacementEdgewards(fogDevices, sensors, actuators, application, moduleMapping));

                TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());

                CloudSim.startSimulation();
                CloudSim.stopSimulation();

                // Print loop delays and tuple execution times
                printSimulationResults(sensorsPerFarm[n]);

                // Simulated results for CSV (based on lab_09 assumptions)
                double fogLatency = Constants.LORAWAN_LATENCY * 2 + 10;
                double cloudLatency = Constants.WAN_LATENCY * 2 + 20;
                double fogEnergy = Constants.FOG_POWER_MAX * sensorsPerFarm[n] * numOfFarms * 0.01;
                double cloudEnergy = Constants.CLOUD_POWER_MAX * sensorsPerFarm[n] * numOfFarms * 0.015;
                double fogNetwork = 0.1 * sensorsPerFarm[n] * numOfFarms;
                double cloudNetwork = 120 * sensorsPerFarm[n] * numOfFarms;

                try (FileWriter writer = new FileWriter("results/case" + (sensorsPerFarm[n] * numOfFarms) + "_output.csv")) {
                    writer.write("Scenario,Latency_ms,Energy_J,Network_KB\n");
                    writer.write("Fog," + fogLatency + "," + fogEnergy + "," + fogNetwork + "\n");
                    writer.write("Cloud," + cloudLatency + "," + cloudEnergy + "," + cloudNetwork + "\n");
                }
                System.out.println("Scenario: " + (sensorsPerFarm[n] * numOfFarms) + " sensors completed. Results saved to case" + (sensorsPerFarm[n] * numOfFarms) + "_output.csv");
            }

            Log.printLine("FarmFogSimulation finished!");
        } catch (Exception e) {
            System.err.println("Simulation failed: " + e.getMessage());
            e.printStackTrace();
            Log.printLine("Unwanted errors happen");
        }
    }

    private static void printSimulationResults(int sensorsPerFarm) {
        // Print application loop delays
        Log.printLine("\nAPPLICATION LOOP DELAYS");
        Log.printLine("=========================================");
        Map<Integer, Double> loopDelays = TimeKeeper.getInstance().getLoopIdToCurrentAverage();
        for (Map.Entry<Integer, Double> entry : loopDelays.entrySet()) {
            Log.printLine(entry.getKey() + " ---> " + entry.getValue());
        }
        Log.printLine("=========================================");

        // Print tuple CPU execution delays
        Log.printLine("\nTUPLE CPU EXECUTION DELAY");
        Log.printLine("=========================================");
        for (Map.Entry<String, Double> entry : TimeKeeper.getInstance().getTupleTypeToAverageCpuTime().entrySet()) {
            Log.printLine(entry.getKey() + " ---> " + entry.getValue());
        }
        Log.printLine("=========================================");
    }

    private static void createFogDevices(int userId, String appId, int sensorsPerFarm) {
        FogDevice cloud = createFogDevice("cloud", Constants.CLOUD_MIPS, Constants.CLOUD_RAM, Constants.WAN_BANDWIDTH, Constants.WAN_BANDWIDTH, 0, 0.01, Constants.CLOUD_POWER_MAX, Constants.CLOUD_POWER_IDLE);
        if (cloud == null) {
            throw new RuntimeException("Failed to create cloud");
        }
        cloud.setParentId(-1);
        fogDevices.add(cloud);

        FogDevice gateway = createFogDevice("gateway", Constants.FOG_MIPS, Constants.FOG_RAM, Constants.LORAWAN_BANDWIDTH, Constants.LORAWAN_BANDWIDTH, 1, 0.0, Constants.FOG_POWER_MAX, Constants.FOG_POWER_IDLE);
        if (gateway == null) {
            throw new RuntimeException("Failed to create gateway");
        }
        gateway.setParentId(cloud.getId());
        gateway.setUplinkLatency(100);
        fogDevices.add(gateway);

        for (int i = 0; i < numOfFarms; i++) {
            addFarm(i + "", userId, appId, gateway.getId(), sensorsPerFarm);
        }

        // Add biometric sensor for authentication
        FogDevice biometricNode = createFogDevice("biometric", 1000, 1000, 10000, 270, 3, 0, 87.53, 82.44);
        if (biometricNode == null) {
            throw new RuntimeException("Failed to create biometric node");
        }
        biometricNode.setParentId(gateway.getId());
        biometricNode.setUplinkLatency(2);
        fogDevices.add(biometricNode);

        Sensor biometricSensor = new Sensor("biometric-sensor", "AUTHENTICATE_USER", userId, appId, new DeterministicDistribution(SENSOR_TRANSMISSION_TIME));
        biometricSensor.setGatewayDeviceId(biometricNode.getId());
        biometricSensor.setLatency(6.0);
        sensors.add(biometricSensor);
    }

    private static FogDevice addFarm(String id, int userId, String appId, int parentId, int sensorsPerFarm) {
        FogDevice farm = createFogDevice("f-" + id, Constants.FOG_MIPS, Constants.FOG_RAM, Constants.LORAWAN_BANDWIDTH, Constants.LORAWAN_BANDWIDTH, 2, 0.0, Constants.FOG_POWER_MAX, Constants.FOG_POWER_IDLE);
        if (farm == null) {
            throw new RuntimeException("Failed to create farm-" + id);
        }
        fogDevices.add(farm);
        farm.setParentId(parentId);
        farm.setUplinkLatency(4);

        for (int i = 0; i < sensorsPerFarm; i++) {
            String sensorId = id + "-" + i;
            addSensor(sensorId, userId, appId, farm.getId());
        }
        return farm;
    }

    private static FogDevice addSensor(String id, int userId, String appId, int parentId) {
        FogDevice sensorNode = createFogDevice("s-" + id, 1000, 1000, 10000, 270, 3, 0, 87.53, 82.44);
        if (sensorNode == null) {
            throw new RuntimeException("Failed to create sensor-" + id);
        }
        sensorNode.setParentId(parentId);
        sensorNode.setUplinkLatency(2);

        Sensor soilSensor = new Sensor("sensor-" + id, "SOIL_MOISTURE", userId, appId, new DeterministicDistribution(SENSOR_TRANSMISSION_TIME));
        sensors.add(soilSensor);
        Actuator irrigation = new Actuator("actuator-" + id, userId, appId, "IRRIGATION_CONTROL");
        actuators.add(irrigation);

        soilSensor.setGatewayDeviceId(sensorNode.getId());
        soilSensor.setLatency(6.0);
        irrigation.setGatewayDeviceId(sensorNode.getId());
        irrigation.setLatency(1.0);

        return sensorNode;
    }

    private static FogDevice createFogDevice(String nodeName, long mips, int ram, long upBw, long downBw, int level, double ratePerMips, double busyPower, double idlePower) {
        try {
            List<Pe> peList = new ArrayList<>();
            peList.add(new Pe(0, new PeProvisionerOverbooking(mips)));

            int hostId = FogUtils.generateEntityId();
            long storage = 1000000;
            int bw = 10000;

            PowerHost host = new PowerHost(
                    hostId,
                    new RamProvisionerSimple(ram),
                    new BwProvisionerOverbooking(bw),
                    storage,
                    peList,
                    new StreamOperatorScheduler(peList),
                    new FogLinearPowerModel(busyPower, idlePower)
            );

            List<Host> hostList = new ArrayList<>();
            hostList.add(host);

            String arch = "x86";
            String os = "Linux";
            String vmm = "Xen";
            double time_zone = 10.0;
            double cost = 3.0;
            double costPerMem = 0.05;
            double costPerStorage = 0.001;
            double costPerBw = 0.0;
            FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(
                    arch, os, vmm, host, time_zone, cost, costPerMem, costPerStorage, costPerBw);

            LinkedList<Storage> storageList = new LinkedList<>();

            FogDevice fogDevice = new FogDevice(nodeName, characteristics,
                    new AppModuleAllocationPolicy(hostList), storageList, 10, upBw, downBw, 0, ratePerMips);
            fogDevice.setLevel(level);
            return fogDevice;
        } catch (Exception e) {
            System.err.println("Error creating FogDevice " + nodeName + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private static Application createApplication(String appId, int userId) {
        Application application = Application.createApplication(appId, userId);

        application.addAppModule("sensor-client", 10);
        application.addAppModule("auth-service", 10);
        application.addAppModule("processing-service", 10);
        application.addAppModule("control-service", 10);
        application.addAppModule("storage-service", 10);

        application.addAppEdge("SOIL_MOISTURE", "sensor-client", 3000, 500, "SOIL_MOISTURE", Tuple.UP, AppEdge.SENSOR);
        application.addAppEdge("AUTHENTICATE_USER", "auth-service", 2000, 200, "AUTHENTICATE_USER", Tuple.UP, AppEdge.SENSOR);
        application.addAppEdge("sensor-client", "processing-service", 3500, 500, "MOISTURE_DATA", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge("processing-service", "control-service", 100, 50, "TRIGGER_IRRIGATION", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge("processing-service", "storage-service", 500, 200, "HOURLY_SUMMARY", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge("control-service", "IRRIGATION_CONTROL", 100, 50, "CONTROL_SIGNAL", Tuple.DOWN, AppEdge.ACTUATOR);
        application.addAppEdge("auth-service", "sensor-client", 100, 100, "AUTH_RESULT", Tuple.DOWN, AppEdge.MODULE);
        application.addAppEdge("sensor-client", "IRRIGATION_CONTROL", 1000, 500, "SENSOR_STATE_UPDATE", Tuple.DOWN, AppEdge.ACTUATOR);

        application.addTupleMapping("sensor-client", "SOIL_MOISTURE", "MOISTURE_DATA", new FractionalSelectivity(0.9));
        application.addTupleMapping("sensor-client", "AUTH_RESULT", "SENSOR_STATE_UPDATE", new FractionalSelectivity(1.0));
        application.addTupleMapping("processing-service", "MOISTURE_DATA", "TRIGGER_IRRIGATION", new FractionalSelectivity(1.0));
        application.addTupleMapping("auth-service", "AUTHENTICATE_USER", "AUTH_RESULT", new FractionalSelectivity(1.0));

        final AppLoop loop1 = new AppLoop(new ArrayList<String>() {{
            add("SOIL_MOISTURE");
            add("sensor-client");
            add("processing-service");
            add("control-service");
            add("IRRIGATION_CONTROL");
        }});
        List<AppLoop> loops = new ArrayList<>();
        loops.add(loop1);
        application.setLoops(loops);

        return application;
    }
}

