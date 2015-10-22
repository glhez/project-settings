package nl.topicus.m2e.settings.internal;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectChangedListener;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SettingsActivator extends Plugin {
	private static final Logger LOGGER = LoggerFactory.getLogger(SettingsActivator.class);
	public static final String PLUGIN_ID = "nl.topicus.m2e.settings";

	// The shared instance
	private static SettingsActivator plugin;
	private IMavenProjectChangedListener m_mavenProjectChangeListener;

	public SettingsActivator() {
		plugin = this;
		System.out.println("nl.topicus.m2e.settings.internal.SettingsActivator");
	}



	@Override
	public void start(BundleContext context) {
		m_mavenProjectChangeListener = new IMavenProjectChangedListener() {

			@Override
			public void mavenProjectChanged(MavenProjectChangedEvent[] events, IProgressMonitor monitor) {
				for (MavenProjectChangedEvent event : events) {
					if(MavenProjectChangedEvent.KIND_CHANGED == event.getKind())
					try {
						ProjectSettingsConfigurator.configure(event.getMavenProject(), monitor);
					} catch (CoreException e) {
						LOGGER.error(e.getMessage(), e);
					}
				}
			}
		};
	MavenPlugin.getMavenProjectRegistry().addMavenProjectChangedListener(m_mavenProjectChangeListener);
}

	@Override
	public void stop(BundleContext context) {
		MavenPlugin.getMavenProjectRegistry().removeMavenProjectChangedListener(m_mavenProjectChangeListener);
	}

	/**
	 * Returns the shared instance.
	 */
	public static SettingsActivator getDefault() {
		return plugin;
	}
}
