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

package org.wildfly.extension.presto;

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

/**
 * @author Heiko Braun
 */
public class PrestoService implements Service<PrestoService> {


    private final String clusterName;

    private final InjectedValue<PathManager> pathManager = new InjectedValue<PathManager>();
    private PrestoServer prestoServer;

    private static final String PRESTO_CONFIG_DIR = "presto/config";

    public PrestoService(String clusterName) {
        this.clusterName = clusterName;
    }

    @Override
    public PrestoService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    @Override
    public void start(StartContext context) throws StartException {

        try {
            PrestoLogger.LOGGER.infof("Starting embedded presto service '%s'", clusterName);

            // create config files (workaround)

            String configPath = resolve(pathManager.getValue(), PRESTO_CONFIG_DIR, ServerEnvironment.SERVER_DATA_DIR) + "/" + clusterName;
            PrestoLogger.LOGGER.infof("Configuration path: '%s'", configPath);


            /*


            these properties need to be written

            coordinator=true
            node-scheduler.include-coordinator=true
            http-server.http.port=8180
            task.max-memory=512MB
            discovery-server.enabled=true
            discovery.uri=http://localhost:8180

            node.environment=test
            node.id=ffffffff-ffff-ffff-ffff-ffffffffffff
            #node.data-dir=/Users/hbraun/dev/prj/heimdall/presto-server-0.97/data

             */

            System.setProperty("config", configPath + "/config.properties");

            prestoServer = new PrestoServer();
            prestoServer.run();

        } catch (Throwable e) {
            context.failed(new StartException(e));
        }
    }

    @Override
    public void stop(StopContext context) {
        PrestoLogger.LOGGER.infof("Stopping presto service '%s'.", clusterName);

        if(prestoServer!=null)
            prestoServer.shutdown();
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
