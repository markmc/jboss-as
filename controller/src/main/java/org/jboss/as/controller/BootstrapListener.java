/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.controller;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartException;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public abstract class BootstrapListener extends AbstractServiceListener<Object> {

    private final AtomicInteger started = new AtomicInteger();
    private final AtomicInteger failed = new AtomicInteger();
    private final AtomicInteger outstanding = new AtomicInteger();
    private final AtomicBoolean done = new AtomicBoolean();
    private final AtomicInteger missingDeps = new AtomicInteger();
    private final EnumMap<ServiceController.Mode, AtomicInteger> map;
    private final ServiceContainer serviceContainer;
    private final Set<ServiceName> missingDepsSet = Collections.synchronizedSet(new TreeSet<ServiceName>());
    private final ServiceTarget serviceTarget;
    private final long startTime;
    private volatile boolean cancelLikely;

    public BootstrapListener(final ServiceContainer serviceContainer, final long startTime, final ServiceTarget serviceTarget) {
        this.serviceContainer = serviceContainer;
        this.startTime = startTime;
        this.serviceTarget = serviceTarget;
        final EnumMap<ServiceController.Mode, AtomicInteger> map = new EnumMap<ServiceController.Mode, AtomicInteger>(ServiceController.Mode.class);
        for (ServiceController.Mode mode : ServiceController.Mode.values()) {
            map.put(mode, new AtomicInteger());
        }
        this.map = map;
    }

    @Override
    public void listenerAdded(final ServiceController<?> controller) {
        final ServiceController.Mode mode = controller.getMode();
        if (mode == ServiceController.Mode.ACTIVE) {
            outstanding.incrementAndGet();
        } else {
            controller.removeListener(this);
        }
        map.get(mode).incrementAndGet();
    }

    @Override
    public void serviceStarted(final ServiceController<?> controller) {
        started.incrementAndGet();
        controller.removeListener(this);
        tick();
    }

    @Override
    public void serviceFailed(final ServiceController<?> controller, final StartException reason) {
        failed.incrementAndGet();
        controller.removeListener(this);
        tick();
    }

    @Override
    public void dependencyFailed(final ServiceController<? extends Object> controller) {
        controller.removeListener(this);
        tick();
    }

    @Override
    public void dependencyUninstalled(final ServiceController<? extends Object> controller) {
        missingDeps.incrementAndGet();
        missingDepsSet.add(controller.getName());
        check();
    }

    @Override
    public void dependencyInstalled(final ServiceController<? extends Object> controller) {
        missingDeps.decrementAndGet();
        missingDepsSet.remove(controller.getName());
        check();
    }

    @Override
    public void serviceRemoved(final ServiceController<?> controller) {
        cancelLikely = true;
        controller.removeListener(this);
        tick();
    }

    private void check() {
        int outstanding = this.outstanding.get();
        if (outstanding == missingDeps.get()) {
            finish(serviceContainer, outstanding);
        }
    }

    private void tick() {
        int outstanding = this.outstanding.decrementAndGet();
        if (outstanding != missingDeps.get()) {
            return;
        }
        finish(serviceContainer, outstanding);
    }

    private void finish(final ServiceContainer container, final int outstanding) {
        if (done.getAndSet(true)) {
            return;
        }
        serviceTarget.removeListener(this);
        if (cancelLikely) {
            return;
        }

        final int failed = this.failed.get() + outstanding;
        final long elapsedTime = Math.max(System.currentTimeMillis() - startTime, 0L);
        final int started = this.started.get();
        done(container, elapsedTime, started, failed, map, missingDepsSet);
    }

    protected abstract void done(final ServiceContainer container, final long elapsedTime, final int started, final int failed, final EnumMap<ServiceController.Mode, AtomicInteger> map, Set<ServiceName> missingDepsSet);
}
