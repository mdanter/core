/**
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.addon.parser.java.ui;

import org.jboss.forge.roaster.model.source.JavaInterfaceSource;
import org.jboss.forge.roaster.model.source.JavaSource;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class JavaInterfaceCommand extends AbstractJavaSourceCommand
{
   @Override
   protected Class<? extends JavaSource<?>> getSourceType()
   {
      return JavaInterfaceSource.class;
   }

   @Override
   protected String getType()
   {
      return "Interface";
   }
}
