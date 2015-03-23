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

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ServiceRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.access.constraint.ApplicationTypeConfig;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.management.ApplicationTypeAccessConstraintDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * The cluster resource definition. Carries the main cassanda configuration attributes.
 *
 * @author Heiko Braun
 * @since 20/08/14
 */
public class NodeDefinition extends PersistentResourceDefinition {

    private final List<AccessConstraintDefinition> accessConstraints;

    private NodeDefinition() {
        super(QueryExtension.CLUSTER_PATH,
                QueryExtension.getResourceDescriptionResolver(QueryModel.NODE),
                NodeAdd.INSTANCE,
                new ServiceRemoveStepHandler(NodeAdd.SERVICE_NAME, NodeAdd.INSTANCE));

        ApplicationTypeConfig atc = new ApplicationTypeConfig(QueryExtension.SUBSYSTEM_NAME, QueryModel.NODE);
        accessConstraints = new ApplicationTypeAccessConstraintDefinition(atc).wrapAsList();
    }

    // -----------
    static final SimpleAttributeDefinition DEBUG =
            new SimpleAttributeDefinitionBuilder(QueryModel.DEBUG, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode(false))
                    .setRestartAllServices()
                    .build();




    // -----------
    static final AttributeDefinition[] ATTRIBUTES = {
            DEBUG
    };

    private static final List CHILDREN = Collections.EMPTY_LIST;

    static final NodeDefinition INSTANCE = new NodeDefinition();

    @Override
    public void registerAttributes(final ManagementResourceRegistration rootResourceRegistration) {
        NodeWriteAttributeHandler handler = new NodeWriteAttributeHandler(ATTRIBUTES);
        for (AttributeDefinition attr : ATTRIBUTES) {
            rootResourceRegistration.registerReadWriteAttribute(attr, null, handler);
        }
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(ATTRIBUTES);
    }

    @Override
    protected List<? extends PersistentResourceDefinition> getChildren() {
        return CHILDREN;
    }

    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        return accessConstraints;
    }


}