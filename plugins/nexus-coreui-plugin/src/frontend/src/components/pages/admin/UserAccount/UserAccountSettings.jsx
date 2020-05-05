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
import React from 'react';
import {useService} from '@xstate/react';
import {Alert, Button, ExtJS, SettingsSection, Textfield} from 'nexus-ui-plugin';
import UIStrings from '../../../../constants/UIStrings';

export default function UserAccountSettings({service}) {
  const [current, send] = useService(service);
  const userId = useUserAccountMachine('userId', service, true);
  const firstName = useUserAccountMachine('firstName', service);
  const lastName = useUserAccountMachine('lastName', service);
  const email = useUserAccountMachine('email', service);
  const context = current.context;
  const isLoading = current.matches('loading') || current.matches('saving');
  const isPristine = context.isPristine;
  const isInvalid = !context.isValid;

  function handleSave(evt) {
    evt.preventDefault();
    send('SAVE');
  }

  function handleDiscard() {
    send('DISCARD');
  }

  let error = null;
  if (context.error instanceof Array) {
    error = (
        <Alert type="error">
          {UIStrings.USER_ACCOUNT.MESSAGES.UPDATE_ERROR}
          <ul>
            {context.error.map(e => <li key={e.id}>{JSON.stringify(e)}</li>)}
          </ul>
        </Alert>
    );
  }
  else if (context.error) {
    error = (
        <Alert type="error">
          {UIStrings.USER_ACCOUNT.MESSAGES.UPDATE_ERROR}<br/>
          {context.error}
        </Alert>
    );
  }

  ExtJS.setDirtyStatus('UserAccount', !isPristine);

  return <SettingsSection isLoading={isLoading}>
    {error}

    <SettingsSection.FieldWrapper labelText={UIStrings.USER_ACCOUNT.ID_FIELD_LABEL}>
      <Textfield {...userId} />
    </SettingsSection.FieldWrapper>
    <SettingsSection.FieldWrapper labelText={UIStrings.USER_ACCOUNT.FIRST_FIELD_LABEL}>
      <Textfield {...firstName} />
    </SettingsSection.FieldWrapper>
    <SettingsSection.FieldWrapper labelText={UIStrings.USER_ACCOUNT.LAST_FIELD_LABEL}>
      <Textfield {...lastName} />
    </SettingsSection.FieldWrapper>
    <SettingsSection.FieldWrapper labelText={UIStrings.USER_ACCOUNT.EMAIL_FIELD_LABEL}>
      <Textfield {...email} />
    </SettingsSection.FieldWrapper>
    <SettingsSection.Footer>
      <Button variant='primary' disabled={isPristine || isInvalid} onClick={handleSave}>
        {UIStrings.SETTINGS.SAVE_BUTTON_LABEL}
      </Button>
      <Button disabled={isPristine} onClick={handleDiscard}>
        {UIStrings.SETTINGS.DISCARD_BUTTON_LABEL}
      </Button>
    </SettingsSection.Footer>
  </SettingsSection>;
}

function useUserAccountMachine(name, service, readOnly=false) {
  const [current, send] = useService(service);
  const value = current.context.data[name];
  const isRequired=!(current.matches('loading') || current.matches('saving'));

  function onChange({target}) {
    send('UPDATE', {
      data: {
        ...current.context.data,
        [target.name]: target.value
      }
    });
  }

  if (readOnly) {
    return {name, value, readOnly};
  }
  else {
    return {name, value, isRequired, onChange};
  }
}
