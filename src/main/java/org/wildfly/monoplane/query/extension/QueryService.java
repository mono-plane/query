/*
 * JBoss, Home of Professional Open Source
 *  Copyright ${year}, Red Hat, Inc., and individual contributors
 *  by the @authors tag. See the copyright.txt in the distribution for a
 *  full listing of individual contributors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wildfly.monoplane.query.extension;

import com.facebook.presto.server.PrestoServer;
import org.jboss.as.controller.services.path.AbsolutePathService;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Properties;
import java.util.UUID;

/**
 * @author Heiko Braun
 */
public class QueryService implements Service<QueryService> {


    private final String clusterName;

    private final InjectedValue<PathManager> pathManager = new InjectedValue<PathManager>();
    private PrestoServer prestoServer;

    private static final String PRESTO_CONFIG_DIR = "presto/config";

    public QueryService(String clusterName) {
        this.clusterName = clusterName;
    }

    @Override
    public QueryService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    @Override
    public void start(StartContext context) throws StartException {

        try {
            QueryLogger.LOGGER.infof("Starting query service '%s'", clusterName);

            // create config files (workaround)

            String moduleDir = resolve(
                    pathManager.getValue(),
                    "modules/system/layers/base/org/wildfly/extension/monoplane/query", // plugin manager scans directories
                    ServerEnvironment.HOME_DIR);

            String configPath = resolve(pathManager.getValue(), PRESTO_CONFIG_DIR, ServerEnvironment.SERVER_DATA_DIR) + "/" + clusterName;
            String catalogPath = configPath + "/catalog";
            QueryLogger.LOGGER.infof("Configuration path: '%s'", configPath);

            // ---

            // create airlift configuration file
            Properties properties = new Properties();
            properties.setProperty("coordinator", "true");
            properties.setProperty("node-scheduler.include-coordinator", "true");
            properties.setProperty("http-server.http.port", "8180");
            properties.setProperty("discovery-server.enabled", "true");
            properties.setProperty("discovery.uri","http://localhost:8180");
            properties.setProperty("node.environment","test");
            properties.setProperty("node.id", UUID.randomUUID().toString());
            properties.setProperty("plugin.config-dir", catalogPath);
            properties.setProperty("plugin.dir", moduleDir);

            createConfigurationFile(configPath, properties, "config.properties");

            // create default cassandra catalog
            Properties connectorProps = new Properties();
            connectorProps.setProperty("connector.name","cassandra");
            connectorProps.setProperty("cassandra.contact-points","localhost");

            createConfigurationFile(catalogPath, connectorProps, "monoplane.properties");


            // ---

            System.setProperty("config", configPath + "/config.properties");

            prestoServer = new PrestoServer();
            prestoServer.run();

        } catch (Throwable e) {
            context.failed(new StartException(e));
        }
    }

    private void createConfigurationFile(String configPath, Properties properties, String fileName) throws Exception {
        // create config dir
        File configDirectory = new File(configPath);
        configDirectory.mkdirs();

        // create dir
        File dir = new File(configPath);
        dir.mkdirs();

        File file = new File(configDirectory, fileName);
        FileOutputStream fileOut = new FileOutputStream(file);
        properties.store(fileOut, "Generated file, don't touch");
        fileOut.close();
    }

    @Override
    public void stop(StopContext context) {
        QueryLogger.LOGGER.infof("Stopping query service '%s'.", clusterName);

        if(prestoServer!=null) {
            prestoServer.shutdown();

            // delete airlift configuration file
        }
    }

    public Injector<PathManager> getPathManagerInjector(){
        return pathManager;
    }

    private String resolve(PathManager pathManager, String path, String relativeToPath) {
        // discard the relativeToPath if the path is absolute and must not be resolved according
        // to the default relativeToPath value
        String relativeTo = AbsolutePathService.isAbsoluteUnixOrWindowsPath(path) ? null : relativeToPath;
        return pathManager.resolveRelativePathEntry(path, relativeTo);
    }

}
