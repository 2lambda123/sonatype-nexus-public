/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
/*global Ext, NX*/

/**
 * Datastore feature panel.
 *
 * @since 3.19
 */
Ext.define('NX.coreui.view.datastore.DatastoreFeature', {
  extend: 'NX.view.drilldown.Drilldown',
  alias: 'widget.nx-coreui-datastore-feature',
  requires: [
    'NX.I18n'
  ],

  /**
   * @override
   */
  initComponent: function() {
    Ext.apply(this, {
      iconName: 'datastore-default',

      masters: [
        { xtype: 'nx-coreui-datastore-list' }
      ],

      tabs: { xtype: 'nx-coreui-datastore-settings' },

      nxActions: [
        { xtype: 'button', text: NX.I18n.get('Datastore_DatastoreFeature_Delete_Button'), glyph: 'xf1f8@FontAwesome' /* fa-trash */, action: 'delete', disabled: true }
      ]
    });

    this.callParent();
  }
});
