//*********************************************************************************/
//Copyright (C) NASA/JPL  California Institute of Technology.                     */
//PDS Imaging Node                                                                */
//All rights reserved.                                                            */
//U.S. Government sponsorship is acknowledged.                                    */
//******************************************************************* *************/
package gov.nasa.pds.imaging.generate;

import gov.nasa.pds.imaging.generate.cli.options.Flag;
import gov.nasa.pds.imaging.generate.cli.options.InvalidOptionException;
import gov.nasa.pds.imaging.generate.label.PDS3Label;
import gov.nasa.pds.imaging.generate.label.PDSObject;
import gov.nasa.pds.imaging.generate.util.ToolInfo;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.xml.transform.TransformerException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;

/**
 * Class used as Command-line interface endpoint. Parses command-line arguments
 * and sends workflow onto the Generator object.
 * 
 * @author jpadams
 * 
 */
public class GenerateLauncher {

    private PDSObject pdsObject = null;
    private String filePath = null;

    private String confPath = null;
    private File templateFile;
    private File outputFile;
    private boolean stdOut;

    private Generator generator;

    public GenerateLauncher() {
        this.templateFile = null;
        this.pdsObject = null;
        this.filePath = null;
        this.confPath = null;
        this.outputFile = null;
        this.stdOut = false;
        this.generator = null;
    }

    /**
     * Displays tool usage.
     * 
     */
    public final void displayHelp() {
        final int maxWidth = 80;
        final HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(maxWidth, "PDS4Generate <options>",
                null, Flag.getOptions(), null);
    }

    /**
     * Displays the current version and disclaimer notice.
     * 
     */
    public final void displayVersion() {
        System.err.println("\n" + ToolInfo.getName());
        System.err.println(ToolInfo.getVersion());
        System.err.println("Release Date: " + ToolInfo.getReleaseDate());
        System.err.println(ToolInfo.getCopyright() + "\n");
    }

    public final void generate() throws Exception {
        this.generator.generate(this.stdOut);
    }

    private String getConfigPath() {
        return (new File(System.getProperty("java.class.path"))).getParentFile().getParent() + "/conf";
    }

    /**
     * A method to parse the command-line arguments.
     * 
     * @param args
     *            The command-line arguments
     * @return A class representation of the command-line arguments
     * 
     * @throws ParseException
     *             If there was an error during parsing.
     */
    public final CommandLine parse(final String[] args) throws ParseException {
        final CommandLineParser parser = new GnuParser();
        return parser.parse(Flag.getOptions(), args);
    }

    /**
     * Examines the command-line arguments passed into the Harvest Tool and
     * takes the appropriate action based on what flags were set.
     * 
     * @param line
     *            A class representation of the command-line arguments.
     * 
     * @throws Exception
     *             If there was an error while querying the options that were
     *             set on the command-line.
     */    
    public final void query(final CommandLine line) throws Exception {
        final List<Option> processedOptions = Arrays.asList(line.getOptions());
        for (final Option o : processedOptions) {
            if (o.getOpt().equals(Flag.HELP.getShortName())) {
                displayHelp();
                System.exit(0);
            }/* else if (o.getOpt().equals(Flag.VERSION.getShortName())) {
                displayVersion();
                System.exit(0);
            }*/ else if (o.getOpt().equals(Flag.PDS3.getShortName())) {
                this.pdsObject = new PDS3Label(getAbsolutePath("PDS3 Label", o.getValue().trim()));
                this.pdsObject.setMappings();
            } else if (o.getOpt().equals(Flag.TEMPLATE.getShortName())) {
                this.templateFile = new File(getAbsolutePath("Velocity Template", o.getValue().trim()));
            }/* else if (o.getOpt().equals(Flag.FILE.getShortName())) {
                this.filePath = o.getValue().trim();
            }*/ else if (o.getOpt().equals(Flag.CONFIG.getShortName())) {
                this.confPath = getAbsolutePath("Config directory", o.getValue().trim());
            } else if (o.getOpt().equals(Flag.OUTPUT.getShortName())) {
                this.outputFile = new File(o.getValue().trim());
            }
        }

        if (this.pdsObject == null) { // Throw error if no PDS3 label is
                                      // specified
            throw new InvalidOptionException("Missing -p flag.  PDS3 label must be specified.");
        }
        if (this.templateFile == null) { // Throw error if no template file
                                         // specified
            throw new InvalidOptionException("Missing -t flag.  Template file must be specified.");
        }
        if (this.outputFile == null) {	// If no outputFile given, default to Standard Out 
        								// Need to set output filename based on
                                       	// label filename
        	this.stdOut = true;
        }
        if (this.confPath == null) { // Need to set output filename based on
                                     // label filename
            this.confPath = getConfigPath();
        }

        // TODO Architectural issue - Too many arguments
        this.generator = new Generator(this.pdsObject, this.templateFile,
                this.filePath, this.confPath, this.outputFile);

    }

	private String getAbsolutePath(String fileType, String filePath) throws InvalidOptionException {
		String finalPath = "";
		File testFile = new File(filePath);
		if (!testFile.isAbsolute()) {
			finalPath = System.getProperty("user.dir") + "/" + filePath;
		} else {
			finalPath = filePath;
		}
		
		if (!(new File(finalPath)).exists()) {
			throw new InvalidOptionException(fileType + " does not exist: " + filePath);
		}
		
		return finalPath;
	}
    
    /**
     * @param args
     */
    public static void main(final String[] args) {
        if (args.length == 0) {
            System.out.println("\nType 'generate -h' for usage");
            System.exit(0);
        }
        try {
            final GenerateLauncher launcher = new GenerateLauncher();
            final CommandLine commandline = launcher.parse(args);
            launcher.query(commandline);
            launcher.generate();
            // launcher.closeHandlers();
        } catch (final ParseException pEx) {
            System.err.println("Command-line parse failure: "
                    + pEx.getMessage());
            System.exit(1);
        } catch (final Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }    

}