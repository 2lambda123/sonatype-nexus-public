/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
/*global NX*/

/**
 * 'textfield' factory.
 *
 * @since 2.7
 */
NX.define('Nexus.capabilities.factory.TextFieldFactory', {

  singleton: true,

  supports: ['textfield', 'string', 'password'],

  /**
   * Creates a textfield.
   * @param formField capability type form field to create textfield for
   * @returns {*} created textfield (never null)
   */
  create: function (formField) {
    var item = {
      xtype: 'textfield',
      renderer: NX.htmlRenderer,
      fieldLabel: formField.label,
      itemCls: formField.required ? 'required-field' : '',
      helpText: formField.helpText,
      allowBlank: formField.required ? false : true,
      regex: formField.regexValidation ? new RegExp(formField.regexValidation) : null,
      anchor: '96%'
    };
    if (formField.type === 'password') {
      item.inputType = 'password';
    }
    if (formField.initialValue) {
      item.value = formField.initialValue;
    }

    if (!formField.enabled) {
      // Makes field look and act disabled, but unlike a disabled field it will submit with the form.
      item.fieldClass = 'x-item-disabled';
      item.readOnly = true;
      // Flag to not toggle the field by enabling/disabling. This would break the customizations.
      item.forceDisabled = true;
    }

    return item;
  }

});
