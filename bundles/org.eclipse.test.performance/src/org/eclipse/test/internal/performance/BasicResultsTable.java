
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

import org.eclipse.test.internal.performance.data.Sample;
import org.eclipse.test.internal.performance.data.ResultsData;

public class BasicResultsTable{
    private static String CURRENT_BUILD, BASELINE_BUILD=null;
    private static ArrayList<Path> inputFiles = new ArrayList<Path>();
    private static String EOL = System.lineSeparator();
    private static String buildDirectory = "";

    public static void main(String[] args) {
        
        parse(args);
        
        //Initialize results data
        ResultsData results = new ResultsData(CURRENT_BUILD, BASELINE_BUILD);
        try {
		    // import data
      	    System.out.println("INFO: Start importing " + inputFiles.size() + " performance data files.");
      	    for (Path inputFile : inputFiles) {
			    results.importData(inputFile);
      	    }
        } catch (Exception ex) {
            System.out.println("Performance data import failed with exception!" + ex);
            System.exit(1);
        }
        
        //Sort all scenarios into components, then make html file per component
        Set<String> scenarioIDs = results.getCurrentScenarios();
        String[] officialComponents = results.getComponents();
        ArrayList<String> usedComponents = new ArrayList<String>();
        HashMap<String, ArrayList<String>> scenarioMap = new HashMap<String, ArrayList<String>>();

        for (String scenarioID : scenarioIDs) {
            String[] scenarioParts = scenarioID.split("\\.");
            String scenarioComponent = "";
            for (String part : scenarioParts) {
                if (part.equals("tests")) {
                    break;
                }
                scenarioComponent = scenarioComponent + part + ".";
            }
            //trim final . 
            
            scenarioComponent = scenarioComponent.substring(0, scenarioComponent.length()-1);

             //check if component in used components list
            if (usedComponents.contains(scenarioComponent)) {
                //Update HashMap entry
                ArrayList<String> componentScenarios = scenarioMap.get(scenarioComponent);
                componentScenarios.add(scenarioID);
                scenarioMap.replace(scenarioComponent, componentScenarios);
            } else {
                //Add component to used components and make new entry into HashMap
                ArrayList<String> componentScenarios = new ArrayList<String>();
                componentScenarios.add(scenarioID);
                scenarioMap.put(scenarioComponent, componentScenarios);
                usedComponents.add(scenarioComponent);
            }
        }

        //Make component html files
        for (String component : usedComponents) {
            ArrayList<String> scenarioList = scenarioMap.get(component);
            scenarioList.sort(String::compareToIgnoreCase);

            //set up html string
            String htmlString = "";
            htmlString = htmlString + EOL + "<h2>Performance of " + component + ": " + CURRENT_BUILD + " relative to " + BASELINE_BUILD + "</h2>" + EOL;
            htmlString = htmlString + EOL + "<a href=\"performance.php?fp_type=0\">Back to global results</a>" + EOL;
            htmlString = htmlString + EOL + "<h3>All " + scenarioList.size() + " scenarios:</h3>" + EOL;
            htmlString = htmlString + EOL + "<p>Times are given in milliseconds.</p>" + EOL;
            htmlString = htmlString + "<table border=\"1\">" + EOL + "<tr>" + EOL;
            htmlString = htmlString + "<td><h4>Class</h4></td>" + EOL;
            htmlString = htmlString + "<td><h4>Name</h4></td>" + EOL;
            htmlString = htmlString + "<td><h4>Elapsed Process (Current)</h4></td>" + EOL;
            htmlString = htmlString + "<td><h4>Elapsed Process (Baseline)</h4></td>" + EOL;
            htmlString = htmlString + "<td><h4>CPU Time (Current)</h4></td>" + EOL;
            htmlString = htmlString + "<td><h4>CPU Time (Baseline)</h4>" + EOL;

            for (String scenario : scenarioList) {
                String[] scenarioParts = scenario.split("#");
                String componentClass = scenarioParts[0];
                String componentName = scenarioParts[1];

                int[] currentData = results.getData("current", scenario);
                int elapsedCurrent = currentData[0];
                int cpuCurrent = currentData[1];

                int[] baselineData = results.getData("baseline", scenario);
                int elapsedBaseline = baselineData[0];
                int cpuBaseline = baselineData[1];

                htmlString = htmlString + "<tr>" + EOL;
                htmlString = htmlString + "<td>" + componentClass + EOL;
                htmlString = htmlString + "<td>" + componentName + EOL;
                htmlString = htmlString + "<td>" + elapsedCurrent + EOL;
                htmlString = htmlString + "<td>" + elapsedBaseline + EOL;
                htmlString = htmlString + "<td>" + cpuCurrent + EOL;
                htmlString = htmlString + "<td>" + cpuBaseline + EOL;
            }

            htmlString = htmlString + "</table>" + EOL;

            //create file
            String outputFileName = buildDirectory + "/" + component + "_BasicTable.html";
            File outputFile = new File(outputFileName);

            try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputFile))){
                outputStream.write(htmlString.getBytes());
            }
            catch (final FileNotFoundException ex) {
                System.err.println("ERROR: File not found exception while writing: " + outputFile.getPath());
                System.exit(1);
            }
            catch (final IOException ex) {
                System.err.println("ERROR: IOException writing: " + outputFile.getPath());
                System.exit(1);
            }

        }

        //make basicResultsIndex.html file
        String htmlString = "";
        htmlString = htmlString + EOL + "<h3 name=\"ScenarioDetail\">Detailed performance data grouped by scenario prefix</h3>" + EOL;

        for (String component : usedComponents) {
            htmlString = htmlString + "<a href=\"./" + component + "_BasicTable.html?fp_type=0\">" + component + "*</a><br>" + EOL;
        }

        //create file
        String outputFileName = buildDirectory + "/BasicResultsIndex.html";
        File outputFile = new File(outputFileName);

        try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputFile))){
            outputStream.write(htmlString.getBytes());
        }
        catch (final FileNotFoundException ex) {
            System.err.println("ERROR: File not found exception while writing: " + outputFile.getPath());
            System.exit(1);
        }
        catch (final IOException ex) {
            System.err.println("ERROR: IOException writing: " + outputFile.getPath());
            System.exit(1);
        }
    }

    //args = baseline, current build, input file array
    private static void parse(String[] args) {
        if (args.length == 0) {
            printUsage();
        }

        int i = 0;
        while (i < args.length) {
            String arg = args[i];
            if (arg.equals("-current")){
                CURRENT_BUILD = args[i+1];
                if (CURRENT_BUILD.startsWith("-")) {
                    System.out.println("Missing value for "+arg+" parameter");
				    printUsage();
                }
                i++;
                i++;
                continue;
            }
            if (arg.equals("-baseline")){
                BASELINE_BUILD = args[i+1];
                if (BASELINE_BUILD.startsWith("-")) {
                    System.out.println("Missing value for "+arg+" parameter");
				    printUsage();
                }
                i++;
                i++;
                continue;
            }
            if (arg.equals("-buildDirectory")){
                buildDirectory = args[i+1];
                if (buildDirectory.startsWith("-")) {
                    System.out.println("Missing value for "+arg+" parameter");
				    printUsage();
                }
                i++;
                i++;
                continue;
            }
            if (arg.equals("-inputFiles")){
                for (int j=1; j < 5; j++) {
                    String inputFile = args[i+j];

                    if (inputFile.startsWith("-")) {
                        System.out.println("Missing value for "+arg+" parameter");
				        printUsage();
                    }
                    //check real file
                    Path inputFilePath = Paths.get(buildDirectory + "/" + inputFile);
                    if (Files.isReadable(inputFilePath)) {
          		        inputFiles.add(inputFilePath);
        	        } else {
          		        System.err.println("ERROR: invalid input argument. Cannot read file: " + inputFile);
        	        } 
      	        }
                i = i+5;
                continue;
            }
            System.err.println("ERROR: Unrecognized argument (arg) found, with value of >" + arg + "<");
            i++;
            continue;
        }

    }

    private static void printUsage() {
        System.out.println(
            "Usage:\n" + 
            "-baseline: build id for the baseline build.\n" +
            "-current: build id for the current build.\n" +
            "-buildDirectory: directory of performance.php file, usually /home/data/httpd/download.eclipse.org/eclipse/downloads/drops4/${BUILD_ID}/performance.\n" +
            "-inputFiles: a list of the dat files from which to extract performance data (will grab the next 4 args as filenames).\n"
            );
        System.exit(1);
    }
}
