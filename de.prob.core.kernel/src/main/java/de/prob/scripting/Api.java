package de.prob.scripting;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;

import de.be4.classicalb.core.parser.exceptions.BException;
import de.prob.animator.IAnimator;
import de.prob.animator.command.StartAnimationCommand;
import de.prob.cli.ProBInstance;
import de.prob.exception.ProBError;
import de.prob.model.classicalb.ClassicalBModel;
import de.prob.statespace.StateSpace;
import de.prob.webconsole.ServletContextListener;

public class Api {

	Logger logger = LoggerFactory.getLogger(Api.class);

	private final FactoryProvider modelFactoryProvider;
	private final Downloader downloader;

	@Override
	public String toString() {
		return "ProB Connector";
	}

	@Inject
	public Api(final FactoryProvider modelFactoryProvider,
			final Downloader downloader) {
		this.modelFactoryProvider = modelFactoryProvider;
		this.downloader = downloader;
	}

	/**
	 * Shutdown the specified {@link ProBInstance} object.
	 * 
	 * @param x
	 */
	public void shutdown(final ProBInstance x) {
		x.shutdown();
	}

	/**
	 * Loads a {@link ClassicalBModel} from the specified file path.
	 * 
	 * @param file
	 * @return classicalBModel
	 * @throws BException
	 * @throws IOException
	 */
	public ClassicalBModel b_load(final String file) throws IOException,
			BException {
		File f = new File(file);
		ClassicalBFactory bFactory = modelFactoryProvider
				.getClassicalBFactory();
		return bFactory.load(f);
	}

	/**
	 * Loads a {@link CSPModel} from the given file. If the user does not have
	 * the cspm parser installed, an Exception is thrown informing the user that
	 * they need to install it.
	 * 
	 * @param file
	 * @return
	 * @throws Exception
	 */
	public CSPModel csp_load(final String file) throws Exception {
		File f = new File(file);
		CSPFactory cspFactory = modelFactoryProvider.getCspFactory();
		CSPModel m = null;
		try {
			m = cspFactory.load(f);
		} catch (ProBError error) {
			throw new Exception(
					"Could find CSP Parser. Perform 'upgrade cspm' to install cspm in your ProB lib directory");
		}
		return m;
	}

	/**
	 * Upgrades the ProB Cli to the given target version
	 * 
	 * @param targetVersion
	 * @return String with the version of the upgrade
	 */
	public String upgrade(final String targetVersion) {
		return downloader.downloadCli(targetVersion);
	}

	/**
	 * Lists the versions of ProB Cli that are available for download
	 * 
	 * @return String with list of possible versions
	 */
	public String listVersions() {
		return downloader.listVersions();
	}

	/**
	 * Writes an xml representation of the StateSpace to file
	 * 
	 * @param s
	 */
	public void toFile(final StateSpace s) {
		XStream xstream = new XStream(new JettisonMappedXmlDriver());
		xstream.omitField(IAnimator.class, "animator");
		// xstream.omitField(History.class, "history");
		String xml = xstream.toXML(s);
		// System.out.println(xml);
		try {
			FileWriter fw = new FileWriter("statespace.xml");
			final BufferedWriter bw = new BufferedWriter(fw);
			bw.write(xml);
			bw.close();
		} catch (IOException e1) {
			System.out.println("could not create file");
		}
	}

	/**
	 * Reads the statespace.xml file and returns the StateSpace that it
	 * represents
	 * 
	 * @return
	 */
	public StateSpace readFile() {
		FileInputStream fstream;
		StringBuffer sb = new StringBuffer();
		try {
			fstream = new FileInputStream("statespace.xml");

			final DataInputStream in = new DataInputStream(fstream);
			final BufferedReader br = new BufferedReader(new InputStreamReader(
					in));

			String tmp;

			try {
				while ((tmp = br.readLine()) != null) {
					sb.append(tmp);
				}
				in.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
		}
		XStream xstream = new XStream(new JettisonMappedXmlDriver());
		// xstream.omitField(IAnimator.class, "animator");

		StateSpace t = (StateSpace) xstream.fromXML(sb.toString());
		IAnimator anim = ServletContextListener.INJECTOR
				.getInstance(IAnimator.class);
		t.setAnimator(anim);
		anim.execute(t.getLoadcmd(), new StartAnimationCommand());

		return t;
	}

	/**
	 * Returns a String representation of the currently available commands for
	 * the Api object. Intended to ease use in the Groovy console.
	 * 
	 * @return
	 */
	public String help() {
		return "Api Commands: \n\n ClassicalBModel b_load(String PathToFile): load .mch files \n"
				+ " CSPModel csp_load(String PathToFile): load .csp files \n"
				+ " upgrade(String version): upgrade ProB cli to specified version\n"
				+ " listVersions(): list currently available ProB cli versions\n"
				+ " toFile(StateSpace s): save StateSpace\n"
				+ " readFile(): reload saved StateSpace\n"
				+ " shutdown(ProBInstance x): shutdown ProBInstance\n"
				+ " help(): print out available commands";
	}
}
