/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.server;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HTTP_INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_INTERFACES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NATIVE_INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_RESOURCES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_TYPES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SCHEMA_LOCATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.server.controller.descriptions.ServerDescriptionConstants.PROFILE_NAME;

import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ExtensionContextImpl;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.common.CommonProviders;
import org.jboss.as.controller.operations.common.NamespaceAddHandler;
import org.jboss.as.controller.operations.common.NamespaceRemoveHandler;
import org.jboss.as.controller.operations.common.SchemaLocationAddHandler;
import org.jboss.as.controller.operations.common.SchemaLocationRemoveHandler;
import org.jboss.as.controller.operations.common.SnapshotDeleteHandler;
import org.jboss.as.controller.operations.common.SnapshotListHandler;
import org.jboss.as.controller.operations.common.SnapshotTakeHandler;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers.StringLengthValidatingHandler;
import org.jboss.as.controller.persistence.ExtensibleConfigurationPersister;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.AttributeAccess.Storage;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.server.controller.descriptions.ServerDescriptionConstants;
import org.jboss.as.server.controller.descriptions.ServerDescriptionProviders;
import org.jboss.as.server.deployment.DeploymentAddHandler;
import org.jboss.as.server.deployment.DeploymentDeployHandler;
import org.jboss.as.server.deployment.DeploymentFullReplaceHandler;
import org.jboss.as.server.deployment.DeploymentRedeployHandler;
import org.jboss.as.server.deployment.DeploymentRemoveHandler;
import org.jboss.as.server.deployment.DeploymentReplaceHandler;
import org.jboss.as.server.deployment.DeploymentUndeployHandler;
import org.jboss.as.server.deployment.DeploymentUploadBytesHandler;
import org.jboss.as.server.deployment.DeploymentUploadStreamAttachmentHandler;
import org.jboss.as.server.deployment.DeploymentUploadURLHandler;
import org.jboss.as.server.deployment.api.DeploymentRepository;
import org.jboss.as.server.operations.ExtensionAddHandler;
import org.jboss.as.server.operations.ExtensionRemoveHandler;
import org.jboss.as.server.operations.HttpManagementAddHandler;
import org.jboss.as.server.operations.NativeManagementAddHandler;
import org.jboss.as.server.operations.ServerOperationHandlers;
import org.jboss.as.server.operations.ServerReloadHandler;
import org.jboss.as.server.operations.ServerShutdownHandler;
import org.jboss.as.server.operations.ServerStateAttributeHandler;
import org.jboss.as.server.operations.SpecifiedPathAddHandler;
import org.jboss.as.server.operations.SpecifiedPathRemoveHandler;
import org.jboss.as.server.operations.SystemPropertyAddHandler;
import org.jboss.as.server.operations.SystemPropertyRemoveHandler;
import org.jboss.as.server.operations.sockets.BindingAddHandler;
import org.jboss.as.server.operations.sockets.BindingFixedPortHandler;
import org.jboss.as.server.operations.sockets.BindingGroupDefaultInterfaceHandler;
import org.jboss.as.server.operations.sockets.BindingInterfaceHandler;
import org.jboss.as.server.operations.sockets.BindingMulticastAddressHandler;
import org.jboss.as.server.operations.sockets.BindingPortHandler;
import org.jboss.as.server.operations.sockets.BindingRemoveHandler;
import org.jboss.as.server.operations.sockets.BindingGroupAddHandler;
import org.jboss.as.server.operations.sockets.BindingGroupPortOffsetHandler;
import org.jboss.as.server.operations.sockets.BindingGroupRemoveHandler;
import org.jboss.as.server.operations.sockets.SpecifiedInterfaceAddHandler;
import org.jboss.as.server.operations.sockets.SpecifiedInterfaceRemoveHandler;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class ServerControllerModelUtil {

    public static ModelNode createCoreModel() {
        ModelNode root = new ModelNode();
        root.get(NAMESPACES).setEmptyList();
        root.get(SCHEMA_LOCATIONS).setEmptyList();
        root.get(NAME);
        root.get(MANAGEMENT_INTERFACES);
        root.get(PROFILE_NAME);
        root.get(SYSTEM_PROPERTIES);
        root.get(EXTENSION);
        root.get(PATH);
        root.get(SUBSYSTEM);
        root.get(INTERFACE);
        root.get(SOCKET_BINDING_GROUP);
        root.get(DEPLOYMENT);
        return root;
    }

    public static void initOperations(final ModelNodeRegistration root, final DeploymentRepository deploymentRepository, final ExtensibleConfigurationPersister extensibleConfigurationPersister, ServerEnvironment serverEnvironment) {
        // Build up the core model registry
        root.registerReadWriteAttribute(NAME, null, new StringLengthValidatingHandler(1), AttributeAccess.Storage.CONFIGURATION);
        // Global operations
        root.registerOperationHandler(GlobalOperationHandlers.ResolveAddressOperationHandler.OPERATION_NAME, GlobalOperationHandlers.RESOLVE, GlobalOperationHandlers.RESOLVE, false, OperationEntry.EntryType.PRIVATE);
        root.registerOperationHandler(READ_RESOURCE_OPERATION, ServerOperationHandlers.SERVER_READ_RESOURCE_HANDLER, CommonProviders.READ_RESOURCE_PROVIDER, true);
        root.registerOperationHandler(READ_ATTRIBUTE_OPERATION, ServerOperationHandlers.SERVER_READ_ATTRIBUTE_HANDLER, CommonProviders.READ_ATTRIBUTE_PROVIDER, true);
        root.registerOperationHandler(READ_RESOURCE_DESCRIPTION_OPERATION, GlobalOperationHandlers.READ_RESOURCE_DESCRIPTION, CommonProviders.READ_RESOURCE_DESCRIPTION_PROVIDER, true);
        root.registerOperationHandler(READ_CHILDREN_NAMES_OPERATION, GlobalOperationHandlers.READ_CHILDREN_NAMES, CommonProviders.READ_CHILDREN_NAMES_PROVIDER, true);
        root.registerOperationHandler(READ_CHILDREN_TYPES_OPERATION, GlobalOperationHandlers.READ_CHILDREN_TYPES, CommonProviders.READ_CHILDREN_TYPES_PROVIDER, true);
        root.registerOperationHandler(READ_CHILDREN_RESOURCES_OPERATION, GlobalOperationHandlers.READ_CHILDREN_RESOURCES, CommonProviders.READ_CHILDREN_RESOURCES_PROVIDER, true);
        root.registerOperationHandler(READ_OPERATION_NAMES_OPERATION, GlobalOperationHandlers.READ_OPERATION_NAMES, CommonProviders.READ_OPERATION_NAMES_PROVIDER, true);
        root.registerOperationHandler(READ_OPERATION_DESCRIPTION_OPERATION, GlobalOperationHandlers.READ_OPERATION_DESCRIPTION, CommonProviders.READ_OPERATION_PROVIDER, true);
        root.registerOperationHandler(WRITE_ATTRIBUTE_OPERATION, ServerOperationHandlers.SERVER_WRITE_ATTRIBUTE_HANDLER, CommonProviders.WRITE_ATTRIBUTE_PROVIDER, true);
        // Other root resource operations
        root.registerOperationHandler(NamespaceAddHandler.OPERATION_NAME, NamespaceAddHandler.INSTANCE, NamespaceAddHandler.INSTANCE, false);
        root.registerOperationHandler(NamespaceRemoveHandler.OPERATION_NAME, NamespaceRemoveHandler.INSTANCE, NamespaceRemoveHandler.INSTANCE, false);
        root.registerOperationHandler(SchemaLocationAddHandler.OPERATION_NAME, SchemaLocationAddHandler.INSTANCE, SchemaLocationAddHandler.INSTANCE, false);
        root.registerOperationHandler(SchemaLocationRemoveHandler.OPERATION_NAME, SchemaLocationRemoveHandler.INSTANCE, SchemaLocationRemoveHandler.INSTANCE, false);
        root.registerOperationHandler(SystemPropertyAddHandler.OPERATION_NAME, SystemPropertyAddHandler.INSTANCE, SystemPropertyAddHandler.INSTANCE, false);
        root.registerOperationHandler(SystemPropertyRemoveHandler.OPERATION_NAME, SystemPropertyRemoveHandler.INSTANCE, SystemPropertyRemoveHandler.INSTANCE, false);
        DeploymentUploadBytesHandler dubh = new DeploymentUploadBytesHandler(deploymentRepository);
        root.registerOperationHandler(DeploymentUploadBytesHandler.OPERATION_NAME, dubh, dubh, false);
        DeploymentUploadURLHandler duuh = new DeploymentUploadURLHandler(deploymentRepository);
        root.registerOperationHandler(DeploymentUploadURLHandler.OPERATION_NAME, duuh, duuh, false);
        DeploymentUploadStreamAttachmentHandler dush = new DeploymentUploadStreamAttachmentHandler(deploymentRepository);
        root.registerOperationHandler(DeploymentUploadStreamAttachmentHandler.OPERATION_NAME, dush, dush, false);
        root.registerOperationHandler(DeploymentReplaceHandler.OPERATION_NAME, DeploymentReplaceHandler.INSTANCE, DeploymentReplaceHandler.INSTANCE, false);
        DeploymentFullReplaceHandler dfrh = new DeploymentFullReplaceHandler(deploymentRepository);
        root.registerOperationHandler(DeploymentFullReplaceHandler.OPERATION_NAME, dfrh, dfrh, false);
        SnapshotDeleteHandler snapshotDelete = new SnapshotDeleteHandler(extensibleConfigurationPersister);
        root.registerOperationHandler(SnapshotDeleteHandler.OPERATION_NAME, snapshotDelete, snapshotDelete, false);
        SnapshotListHandler snapshotList = new SnapshotListHandler(extensibleConfigurationPersister);
        root.registerOperationHandler(SnapshotListHandler.OPERATION_NAME, snapshotList, snapshotList, false);
        SnapshotTakeHandler snapshotTake = new SnapshotTakeHandler(extensibleConfigurationPersister);
        root.registerOperationHandler(SnapshotTakeHandler.OPERATION_NAME, snapshotTake, snapshotTake, false);

        root.registerReadOnlyAttribute(ServerDescriptionConstants.SERVER_STATE, ServerStateAttributeHandler.INSTANCE, Storage.RUNTIME);

//        root.registerOperationHandler(ServerCompositeOperationHandler.OPERATION_NAME, ServerCompositeOperationHandler.INSTANCE, ServerCompositeOperationHandler.INSTANCE, false);

        // Runtime operations
        root.registerOperationHandler(ServerReloadHandler.OPERATION_NAME, ServerReloadHandler.INSTANCE, ServerReloadHandler.INSTANCE, false);

        if (serverEnvironment != null && serverEnvironment.isStandalone())
            root.registerOperationHandler(ServerShutdownHandler.OPERATION_NAME, ServerShutdownHandler.INSTANCE, ServerShutdownHandler.INSTANCE, false);

        // Management API protocols
        ModelNodeRegistration managementNative = root.registerSubModel(PathElement.pathElement(MANAGEMENT_INTERFACES, NATIVE_INTERFACE), CommonProviders.MANAGEMENT_PROVIDER);
        managementNative.registerOperationHandler(NativeManagementAddHandler.OPERATION_NAME, NativeManagementAddHandler.INSTANCE, NativeManagementAddHandler.INSTANCE, false);

        ModelNodeRegistration managementHttp = root.registerSubModel(PathElement.pathElement(MANAGEMENT_INTERFACES, HTTP_INTERFACE), CommonProviders.MANAGEMENT_PROVIDER);
        managementHttp.registerOperationHandler(HttpManagementAddHandler.OPERATION_NAME, HttpManagementAddHandler.INSTANCE, HttpManagementAddHandler.INSTANCE, false);
        // root.registerReadWriteAttribute(ModelDescriptionConstants.MANAGEMENT_INTERFACES, GlobalOperationHandlers.READ_ATTRIBUTE, ManagementSocketAddHandler.INSTANCE);

        // Paths
        ModelNodeRegistration paths = root.registerSubModel(PathElement.pathElement(PATH), CommonProviders.SPECIFIED_PATH_PROVIDER);
        paths.registerOperationHandler(SpecifiedPathAddHandler.OPERATION_NAME, SpecifiedPathAddHandler.INSTANCE, SpecifiedPathAddHandler.INSTANCE, false);
        paths.registerOperationHandler(SpecifiedPathRemoveHandler.OPERATION_NAME, SpecifiedPathRemoveHandler.INSTANCE, SpecifiedPathRemoveHandler.INSTANCE, false);

        // Interfaces
        ModelNodeRegistration interfaces = root.registerSubModel(PathElement.pathElement(INTERFACE), CommonProviders.SPECIFIED_INTERFACE_PROVIDER);
        interfaces.registerOperationHandler(SpecifiedInterfaceAddHandler.OPERATION_NAME, SpecifiedInterfaceAddHandler.INSTANCE, SpecifiedInterfaceAddHandler.INSTANCE, false);
        interfaces.registerOperationHandler(SpecifiedInterfaceRemoveHandler.OPERATION_NAME, SpecifiedInterfaceRemoveHandler.INSTANCE, SpecifiedInterfaceRemoveHandler.INSTANCE, false);

        // Sockets
        ModelNodeRegistration socketGroup = root.registerSubModel(PathElement.pathElement(SOCKET_BINDING_GROUP), ServerDescriptionProviders.SOCKET_BINDING_GROUP_PROVIDER);
        socketGroup.registerOperationHandler(BindingGroupAddHandler.OPERATION_NAME, BindingGroupAddHandler.INSTANCE, BindingGroupAddHandler.INSTANCE, false);
        socketGroup.registerOperationHandler(BindingGroupRemoveHandler.OPERATION_NAME, BindingGroupRemoveHandler.INSTANCE, BindingGroupRemoveHandler.INSTANCE, false);
        socketGroup.registerReadWriteAttribute(PORT_OFFSET, null, BindingGroupPortOffsetHandler.INSTANCE, AttributeAccess.Storage.CONFIGURATION);
        socketGroup.registerReadWriteAttribute(DEFAULT_INTERFACE, null, BindingGroupDefaultInterfaceHandler.INSTANCE, AttributeAccess.Storage.CONFIGURATION);
        ModelNodeRegistration socketBinding = socketGroup.registerSubModel(PathElement.pathElement(SOCKET_BINDING), CommonProviders.SOCKET_BINDING_PROVIDER);
        socketBinding.registerOperationHandler(BindingAddHandler.OPERATION_NAME, BindingAddHandler.INSTANCE, BindingAddHandler.INSTANCE, false);
        socketBinding.registerOperationHandler(BindingRemoveHandler.OPERATION_NAME, BindingRemoveHandler.INSTANCE, BindingRemoveHandler.INSTANCE, false);
        socketBinding.registerReadWriteAttribute(INTERFACE, null, BindingInterfaceHandler.INSTANCE, AttributeAccess.Storage.CONFIGURATION);
        socketBinding.registerReadWriteAttribute(PORT, null, BindingPortHandler.INSTANCE, AttributeAccess.Storage.CONFIGURATION);
        socketBinding.registerReadWriteAttribute(FIXED_PORT, null, BindingFixedPortHandler.INSTANCE, AttributeAccess.Storage.CONFIGURATION);
        socketBinding.registerReadWriteAttribute(MULTICAST_ADDRESS, null, BindingMulticastAddressHandler.INSTANCE, AttributeAccess.Storage.CONFIGURATION);
        socketBinding.registerReadWriteAttribute(MULTICAST_PORT, null, BindingPortHandler.INSTANCE, AttributeAccess.Storage.CONFIGURATION);

        // Deployments
        ModelNodeRegistration deployments = root.registerSubModel(PathElement.pathElement(DEPLOYMENT), ServerDescriptionProviders.DEPLOYMENT_PROVIDER);
        DeploymentAddHandler dah = new DeploymentAddHandler(deploymentRepository);
        deployments.registerOperationHandler(DeploymentAddHandler.OPERATION_NAME, dah, dah, false);
        deployments.registerOperationHandler(DeploymentRemoveHandler.OPERATION_NAME, DeploymentRemoveHandler.INSTANCE, DeploymentRemoveHandler.INSTANCE, false);
        deployments.registerOperationHandler(DeploymentDeployHandler.OPERATION_NAME, DeploymentDeployHandler.INSTANCE, DeploymentDeployHandler.INSTANCE, false);
        deployments.registerOperationHandler(DeploymentUndeployHandler.OPERATION_NAME, DeploymentUndeployHandler.INSTANCE, DeploymentUndeployHandler.INSTANCE, false);
        deployments.registerOperationHandler(DeploymentRedeployHandler.OPERATION_NAME, DeploymentRedeployHandler.INSTANCE, DeploymentRedeployHandler.INSTANCE, false);

        // Extensions
        ModelNodeRegistration extensions = root.registerSubModel(PathElement.pathElement(EXTENSION), CommonProviders.EXTENSION_PROVIDER);
        ExtensionContext extensionContext = new ExtensionContextImpl(root, deployments, extensibleConfigurationPersister);
        ExtensionAddHandler addExtensionHandler = new ExtensionAddHandler(extensionContext);
        extensions.registerOperationHandler(ExtensionAddHandler.OPERATION_NAME, addExtensionHandler, addExtensionHandler, false);
        extensions.registerOperationHandler(ExtensionRemoveHandler.OPERATION_NAME, ExtensionRemoveHandler.INSTANCE, ExtensionRemoveHandler.INSTANCE, false);

    }
}
