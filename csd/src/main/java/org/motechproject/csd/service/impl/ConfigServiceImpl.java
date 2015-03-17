package org.motechproject.csd.service.impl;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.motechproject.config.core.constants.ConfigurationConstants;
import org.motechproject.csd.domain.Config;
import org.motechproject.csd.service.ConfigService;
import org.motechproject.event.MotechEvent;
import org.motechproject.event.listener.annotations.MotechListener;
import org.motechproject.server.config.SettingsFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service("configService")
public class ConfigServiceImpl implements ConfigService {

    private static final String CSD_CONFIG_FILE_NAME = "csd-config.json";
    private static final String CSD_CONFIG_FILE_PATH = "/" + ConfigurationConstants.RAW_DIR + "/" +
            CSD_CONFIG_FILE_NAME;
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigServiceImpl.class);
    private SettingsFacade settingsFacade;
    private Config config;

    private synchronized void loadConfigs() {
        try (InputStream is = settingsFacade.getRawConfig(CSD_CONFIG_FILE_NAME)) {
            String jsonText = IOUtils.toString(is);
            Gson gson = new Gson();
            config = gson.fromJson(jsonText, Config.class);
        }
        catch (Exception e) {
            throw new JsonIOException("Malformed " + CSD_CONFIG_FILE_NAME + " file? " + e.toString(), e);
        }
    }

    @Autowired
    public ConfigServiceImpl(@Qualifier("csdSettings") SettingsFacade settingsFacade) {
        this.settingsFacade = settingsFacade;
        loadConfigs();
    }

    @MotechListener(subjects = { ConfigurationConstants.FILE_CHANGED_EVENT_SUBJECT })
    public void handleFileChanged(MotechEvent event) {
        String filePath = (String) event.getParameters().get(ConfigurationConstants.FILE_PATH);
        if (!StringUtils.isBlank(filePath) && filePath.endsWith(CSD_CONFIG_FILE_PATH)) {
            LOGGER.info("{} has changed, reloading configs.", CSD_CONFIG_FILE_NAME);
            loadConfigs();
        }
    }

    @Override
    public Config getConfig() {
        return config;
    }

    @Override
    public void updateConfig(Config config) {
        Gson gson = new Gson();
        String jsonText = gson.toJson(config, Config.class);
        ByteArrayResource resource = new ByteArrayResource(jsonText.getBytes());
        settingsFacade.saveRawConfig(CSD_CONFIG_FILE_NAME, resource);
        loadConfigs();
    }
}
