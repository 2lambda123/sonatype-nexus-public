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
package org.sonatype.nexus.repository.rest.api.model;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * @since 3.next
 */
public abstract class AbstractRepositoryApiRequest
{
  @ApiModelProperty(value = "A unique identifier for this repository", example = "internal")
  @NotEmpty
  protected String name;

  @ApiModelProperty(value = "Component format used by this repository", example = "npm")
  @NotEmpty
  protected String format;

  @ApiModelProperty(value = "Controls if deployments of and updates to artifacts are allowed",
      allowableValues = "hosted,proxy,group", example = "hosted")
  @NotEmpty
  protected String type;

  @ApiModelProperty(value = "Whether this repository accepts incoming requests", example = "true")
  @NotNull
  protected Boolean online;

  @JsonCreator
  public AbstractRepositoryApiRequest(
      @JsonProperty("name") final String name,
      @JsonProperty("format") final String format,
      @JsonProperty("type") final String type,
      @JsonProperty("online") final Boolean online)
  {
    this.name = name;
    this.format = format;
    this.type = type;
    this.online = online;
  }

  public String getName() {
    return name;
  }

  public String getFormat() {
    return format;
  }

  public String getType() {
    return type;
  }

  public Boolean getOnline() {
    return online;
  }
}
