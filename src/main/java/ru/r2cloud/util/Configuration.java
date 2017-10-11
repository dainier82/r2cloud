package ru.r2cloud.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.ddns.DDNSType;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

public class Configuration {

	private static final Logger LOG = LoggerFactory.getLogger(Configuration.class);

	private final Properties userSettings = new Properties();
	private final String userSettingsLocation;
	private static Set<PosixFilePermission> MODE600 = new HashSet<PosixFilePermission>();

	private final Properties systemSettings = new Properties();
	private final Map<String, ConfigListener> listeners = new ConcurrentHashMap<String, ConfigListener>();
	private final Set<String> changedProperties = new HashSet<String>();

	static {
		MODE600.add(PosixFilePermission.OWNER_READ);
		MODE600.add(PosixFilePermission.OWNER_WRITE);
	}

	public Configuration(String systemSettingsLocation, String userSettingsLocation) {
		try (InputStream is = new FileInputStream(systemSettingsLocation)) {
			systemSettings.load(is);
		} catch (Exception e) {
			throw new RuntimeException("Unable to load properties", e);
		}
		this.userSettingsLocation = userSettingsLocation;
		if (new File(userSettingsLocation).exists()) {
			try (InputStream is = new FileInputStream(userSettingsLocation)) {
				userSettings.load(is);
			} catch (Exception e) {
				throw new RuntimeException("Unable to load user properties", e);
			}
		}
	}
	
	public String setProperty(String key, Long value) {
		return setProperty(key, String.valueOf(value));
	}

	public String setProperty(String key, Integer value) {
		return setProperty(key, String.valueOf(value));
	}
	
	public String setProperty(String key, boolean value) {
		return setProperty(key, String.valueOf(value));
	}

	public String setProperty(String key, String value) {
		synchronized (changedProperties) {
			changedProperties.add(key);
		}
		return (String) userSettings.put(key, value);
	}

	public void update() {
		try (FileWriter fos = new FileWriter(userSettingsLocation)) {
			userSettings.store(fos, "updated");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		try {
			Files.setPosixFilePermissions(Paths.get(userSettingsLocation), MODE600);
		} catch (IOException e) {
			LOG.info("unable to setup 600 permissions: " + e.getMessage());
		}
		Set<ConfigListener> toNotify = new HashSet<ConfigListener>();
		synchronized (changedProperties) {
			for (String cur : changedProperties) {
				ConfigListener curListener = listeners.get(cur);
				if (curListener == null) {
					continue;
				}
				toNotify.add(curListener);
			}
			changedProperties.clear();
		}

		for (ConfigListener cur : toNotify) {
			try {
				cur.onConfigUpdated();
			} catch (Exception e) {
				LOG.error("unable to notify listener: " + cur, e);
			}
		}
	}

	public long getThreadPoolShutdownMillis() {
		return getLong("threadpool.shutdown.millis");
	}

	public Long getLong(String name) {
		String strValue = getProperty(name);
		if (strValue == null) {
			return null;
		}
		return Long.valueOf(strValue);
	}

	public Integer getInteger(String name) {
		String strValue = getProperty(name);
		if (strValue == null) {
			return null;
		}
		return Integer.valueOf(strValue);
	}

	public boolean getBoolean(String string) {
		String str = getProperty(string);
		if (str == null) {
			return false;
		}
		return Boolean.valueOf(str);
	}

	public Double getDouble(String name) {
		String str = getProperty(name);
		if (str == null) {
			return null;
		}
		return Double.valueOf(str);
	}

	public String getProperty(String name) {
		String result = userSettings.getProperty(name);
		if (result != null) {
			return result;
		}
		return systemSettings.getProperty(name);
	}

	public DDNSType getDdnsType(String name) {
		String str = getProperty(name);
		if (str == null || str.trim().length() == 0) {
			return null;
		}
		return DDNSType.valueOf(str);
	}

	public void remove(String name) {
		userSettings.remove(name);
	}

	public List<String> getList(String name) {
		String str = getProperty(name);
		if (str == null) {
			return Collections.emptyList();
		}
		return Lists.newArrayList(Splitter.on(',').trimResults().omitEmptyStrings().split(str));
	}

	public void subscribe(ConfigListener listener, String... names) {
		for (String cur : names) {
			this.listeners.put(cur, listener);
		}
	}

}