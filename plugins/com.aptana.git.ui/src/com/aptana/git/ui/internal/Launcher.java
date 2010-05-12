package com.aptana.git.ui.internal;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.ui.externaltools.internal.model.IExternalToolConstants;

import com.aptana.console.process.ConsoleProcessFactory;
import com.aptana.core.ShellExecutable;
import com.aptana.git.core.GitPlugin;

/**
 * Launches a process through Eclipse's launching infrastructure, launching it into the console.
 * 
 * @author cwilliams
 */
@SuppressWarnings("restriction")
public abstract class Launcher
{
	/**
	 * Invalid characters for use in a launch configuration name.
	 */
	private static final char[] INVALID_CHARS = new char[] { '@', '&', '\\', '/', ':', '*', '?', '"', '<', '>', '|',
			'\0' };

	/**
	 * @param command
	 * @param workingDir
	 * @param args
	 * @return
	 */
	public static ILaunch launch(String command, IPath workingDir, String... args) throws CoreException
	{
		return launch(command, workingDir, new NullProgressMonitor(), args);
	}

	public static ILaunch launch(String command, IPath workingDir, IProgressMonitor monitor, String... args)
			throws CoreException
	{
		ILaunchConfigurationWorkingCopy config = createLaunchConfig(command, workingDir, args);
		return config.launch(ILaunchManager.RUN_MODE, monitor);
	}

	// TODO 3.6+ Can't properly point to undeprecated constants until 3.6 is our base version where they moved these out
	// to a core plugin
	// @SuppressWarnings("deprecation")
	private static ILaunchConfigurationWorkingCopy createLaunchConfig(String command, IPath workingDir, String... args)
			throws CoreException
	{
		ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType configType = manager
				.getLaunchConfigurationType(IExternalToolConstants.ID_PROGRAM_BUILDER_LAUNCH_CONFIGURATION_TYPE);
		String name = getLastPortion(command) + " " + join(args, " "); //$NON-NLS-1$ //$NON-NLS-2$
		// if 3.6M6+
		try
		{
			// name = manager.generateLaunchConfigurationName(name);
			Method m = ILaunchManager.class.getMethod("generateLaunchConfigurationName", String.class); //$NON-NLS-1$
			name = (String) m.invoke(manager, name);
		}
		catch (Exception e)
		{
			// ignore exception, we must be on Eclipse < 3.6M6
			// TODO Remove this code when 3.6 is our base platform
			for (char c : INVALID_CHARS)
			{
				name = name.replace(c, '_');
			}
			name = manager.generateUniqueLaunchConfigurationNameFrom(name);
		}

		List<String> shellCommand = ShellExecutable.toShellCommand(command, args);
		command = shellCommand.remove(0);
		args = shellCommand.toArray(new String[shellCommand.size()]);

		String toolArgs = '"' + join(args, "\" \"") + '"'; //$NON-NLS-1$
		ILaunchConfigurationWorkingCopy config = configType.newInstance(null, name);
		config.setAttribute(IExternalToolConstants.ATTR_LOCATION, command);
		config.setAttribute(IExternalToolConstants.ATTR_TOOL_ARGUMENTS, toolArgs);
		config.setAttribute(IExternalToolConstants.ATTR_WORKING_DIRECTORY, workingDir.toOSString());
		config.setAttribute(DebugPlugin.ATTR_CAPTURE_OUTPUT, true);
		config.setAttribute(IExternalToolConstants.ATTR_SHOW_CONSOLE, true);
		config.setAttribute(IDebugUIConstants.ATTR_LAUNCH_IN_BACKGROUND, false);
		config.setAttribute(DebugPlugin.ATTR_PROCESS_FACTORY_ID, ConsoleProcessFactory.ID);
		Map<String, String> env = new HashMap<String, String>();
		IPath git_ssh = GitPlugin.getDefault().getGIT_SSH();
		if (git_ssh != null)
		{
			env.put("GIT_SSH", git_ssh.toOSString()); //$NON-NLS-1$
		}
		if (!env.isEmpty())
		{
			config.setAttribute(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES, env);
			config.setAttribute(ILaunchManager.ATTR_APPEND_ENVIRONMENT_VARIABLES, true);
		}
		return config;
	}

	private static String getLastPortion(String command)
	{
		return new Path(command).lastSegment();
	}

	private static String join(String[] commands, String delimiter)
	{
		StringBuilder builder = new StringBuilder();
		for (String command : commands)
		{
			builder.append(command).append(delimiter);
		}
		builder.delete(builder.length() - delimiter.length(), builder.length());
		return builder.toString();
	}
}
