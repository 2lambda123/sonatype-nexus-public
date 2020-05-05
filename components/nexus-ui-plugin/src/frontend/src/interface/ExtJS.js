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
 * @since 3.next
 */
export default class {
  /**
   * Open a success message notification
   * @param text
   */
  static showSuccessMessage(text) {
    NX.Messages.success(text)
  }

  /**
   * Open an error message notification
   * @param text
   */
  static showErrorMessage(text) {
    NX.Messages.error(text);
  }

  /**
   * Set the global dirty status to prevent accidental navigation
   * @param key - a unique key for the view that is dirty
   * @param isDirty - whether the view is dirty or not
   */
  static setDirtyStatus(key, isDirty) {
    window.dirty = window.dirty || [];

    if (isDirty) {
      window.dirty.push(key)
    }
    else {
      window.dirty = window.dirty.filter(it => it !== key)
    }
  }
}
