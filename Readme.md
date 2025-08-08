# FarmFog Simulation Project

## 1. Project Overview

This project is a proof-of-concept simulation of the *FarmFog* architecture, a fog computing framework for IoT-based smart agriculture. The simulation is built using the **iFogSim** toolkit and is designed to evaluate the performance of a fog-based system against a traditional cloud-only model.

The primary goal is to analyze key performance metrics, including:

- Execution Time / Latency
- Total Energy Consumption
- Network Usage

The simulation is based on the research paper: *"FarmFog: The Implementation of Fog Computing and Biometric System in IoT-based Smart Agriculture"* by Rajakumar & Kumar (2025).

## 2. System Requirements

- **Java Development Kit (JDK)**: Version 1.8
- **An IDE**: Eclipse is recommended
- **Python 3**: For running the results plotting script


## 3. Setup and Execution

### iFogSim Setup

The core simulation environment and methodology for this project are based on the concepts and examples covered in the `lab_09.pdf`. This includes the approach for defining the physical topology, creating application modules, defining application loops for latency measurement, and comparing "Edge-ward" vs. "Cloud-only" placement strategies.

### Running the Simulation

1. Clone this repository:
   
2. Copy all .java files to folder "org.fog.test.perfeval" in the eclipse iFog project

3. Switch between the Fog-based and Cloud-only models, by changing the CLOUD boolean flag at the top of the FarmFogSimulation.java file and run the FarmFogSimulation.java file.

4. Update the data arrays in the python script with the latest values from simulation.

5. Install the script requirements:
 ```
 pip install requirements.txt
 ```

6. Run the script from your terminal:
 ```
 python plot_results.py
 ```

7. The script will generate PNG images
