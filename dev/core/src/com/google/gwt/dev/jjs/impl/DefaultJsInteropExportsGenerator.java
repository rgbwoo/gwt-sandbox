/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.jjs.impl;

import static com.google.gwt.dev.js.JsUtils.createAssignment;

import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.ast.HasName;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JMember;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsInvocation;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsNameRef;
import com.google.gwt.dev.js.ast.JsStatement;
import com.google.gwt.dev.js.ast.JsStringLiteral;

import java.util.List;
import java.util.Map;

/**
 * Responsible for handling @JsExport code generation for non-Closure formatted code.
 * <p>
 * Generally, export of global namespaced members looks like this:
 * <pre>
 * _ = provide('dotted.namespace')
 * _.memberName = original
 * </pre>
 * Essentially members are aliased into a global namespace.
 */
class DefaultJsInteropExportsGenerator implements JsInteropExportsGenerator {

  private final List<JsStatement> exportStmts;
  private final Map<HasName, JsName> names;
  private final JsName globalTemp;
  private final JsName provideFunc;
  private String lastExportedNamespace;

  public DefaultJsInteropExportsGenerator(List<JsStatement> exportStmts, Map<HasName, JsName> names,
      JsName globalTemp, Map<String, JsFunction> indexedFunctions) {
    this.exportStmts = exportStmts;
    this.names = names;
    this.globalTemp = globalTemp;
    this.provideFunc = indexedFunctions.get("JavaClassHierarchySetupUtil.provide").getName();
  }

  @Override
  public void exportType(JDeclaredType x) {
    // non-Closure mode doesn't do anything special to export types
  }

  /*
   * Exports a member as
   *  _ = provide('foo.bar.ExportNamespace')
   *  _.memberName = RHS
   *
   * TODO(goktug): optimizing provide calls shouldn't be difficult as exports are now sorted.
   */
  @Override
  public void exportMember(JMember x) {
    // _ = provide('foo.bar.ExportNamespace')
    ensureProvideNamespace(x.getExportNamespace(), x.getSourceInfo());

    // _.memberName = RHS
    JsNameRef lhs = new JsNameRef(x.getSourceInfo(), x.getExportName());
    lhs.setQualifier(globalTemp.makeRef(x.getSourceInfo()));
    JsNameRef rhs = names.get(x).makeRef(x.getSourceInfo());
    exportStmts.add(createAssignment(lhs, rhs).makeStmt());
  }

  private void ensureProvideNamespace(String namespace, SourceInfo sourceInfo) {
    if (namespace.equals(lastExportedNamespace)) {
      return;
    }
    lastExportedNamespace = namespace;

    // _ = JCHSU.provide('foo.bar')
    JsInvocation provideCall = new JsInvocation(sourceInfo);
    provideCall.setQualifier(provideFunc.makeRef(sourceInfo));
    provideCall.getArguments().add(new JsStringLiteral(sourceInfo, namespace));
    exportStmts.add(createAssignment(globalTemp.makeRef(sourceInfo), provideCall).makeStmt());
  }
}
