
# TV Commercial Scheduling

This project implements the **Commercial Scheduling Problem** as described in the accompanying paper. The datasets required for this project are located in the `instances` folder.

## Installation

### Step 0: Clone the Repository

Begin by cloning the repository with the following command:

```bash
git clone https://github.com/OEKundakcioglu/TVCommercialScheduling.git
```

### Step 1: Running the Code

The project provides Gradle tasks to execute the GRASP and MIP algorithms. Below are the steps to replicate the results from the paper or run the algorithms with custom instances.

#### 1.1: Running the GRASP Code

To execute the GRASP algorithm, you can use one of the following tasks:

##### 1.1.1: Replicating Paper Results

To replicate the results presented in the paper, use the following command:

**On macOS/Linux:**

```bash
./gradlew runLoop -PyamlConfigPath=path/to/config.yaml
```

**On Windows:**

```bash
gradlew runLoop -PyamlConfigPath=path\to\config.yaml
```

The `runLoop` task runs the GRASP algorithm using the specified YAML configuration file. An example configuration file (`yamlConfigGrasp.yaml`) is provided, containing default values to replicate the results in the paper. You may use this as a template for your own configurations.

##### 1.1.2: Running the GRASP Code with a Single Instance

To run the GRASP algorithm on a single instance, use the following command:

**On macOS/Linux:**

```bash
./gradlew runHeuristic -PisBestMove=true -PalphaGeneratorType=Uniform -PalphaGeneratorRange=0.1,0.9 -PinstancePath=path/to/instance -PtimeLimit=3600 -PoutputPath=path/to/output
```

**On Windows:**

```bash
gradlew runHeuristic -PisBestMove=true -PalphaGeneratorType=Uniform -PalphaGeneratorRange=0.1,0.9 -PinstancePath=path\to\instance -PtimeLimit=3600 -PoutputPath=path\to\output
```

Where:
- `isBestMove`: Specifies whether to use the best move strategy (`true` or `false`).
- `alphaGeneratorType`: Specifies the type of alpha generator, either `Uniform` (U) or `Fixed` (F).
- `alphaGeneratorRange`: Defines the range of alpha values in the form of `"start,end"` if `alphaGeneratorType` is `Uniform`, otherwise a single value.
- `instancePath`: The path to the instance file.
- `timeLimit`: The time limit for the execution, in seconds.
- `outputPath` (Optional): The path to the output file.

##### Example Usage

To run the GRASP algorithm with default configurations:

```bash
./gradlew runHeuristic -PisBestMove=true -PalphaGeneratorType=Fixed -PalphaGeneratorRange=0.5 -PinstancePath=instances/example_instance.yaml -PtimeLimit=3600 -PoutputPath=results/output.json
```

This command will execute the heuristic using a fixed alpha value of 0.5 on the specified instance with a time limit of 3600 seconds, saving the results to `results/output.json`.

#### 1.2: Running the MIP Code

To execute the MIP algorithm, you can use one of the following tasks:

##### 1.2.1: Replicating Paper Results

Similarly, to replicate the MIP results presented in the paper, use the following command:

**On macOS/Linux:**

```bash
./gradlew runMipLoop -PyamlConfigPath=path/to/config.yaml
```

**On Windows:**

```bash
gradlew runMipLoop -PyamlConfigPath=path\to\config.yaml
```

The `runMipLoop` task runs the MIP algorithm using the specified YAML configuration file. An example configuration file (`yamlConfigMip.yaml`) is provided, which you can use to replicate the paper's results.

##### 1.2.2: Running the MIP Code with a Single Instance

To run the MIP algorithm on a single instance, use the following command:

**On macOS/Linux:**

```bash
./gradlew runMip -PinstancePath=path/to/instance -PtimeLimit=3600 -PoutputPath=path/to/output
```

**On Windows:**

```bash
gradlew runMip -PinstancePath=path\to\instance -PtimeLimit=3600 -PoutputPath=path\to\output
```

Where:
- `instancePath`: The path to the instance file.
- `timeLimit`: The time limit for the execution, in seconds.
- `outputPath` (Optional): The path to the output file.

