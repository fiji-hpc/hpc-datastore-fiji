/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2020 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.register_service;

import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.OneToMany;

import cz.it4i.fiji.datastore.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity(name = "Dataset")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Dataset extends BaseEntity {

 private static final long serialVersionUID = 4257740020931057972L;

	@Getter
	@Setter
	private UUID uuid;

	@Getter
	@Setter
	@ElementCollection(targetClass = ResolutionLevel.class)
	private Collection<ResolutionLevel> resolutionLevel;
	
	@Getter
	@Setter
	private String path;

	@Getter
	@Setter
	@OneToMany
	private Collection<DatasetVersion> datasetVersion;
	
	@Getter
	@Setter
	@OneToMany
	private Collection<ViewSetup> viewSetup;
	
//TODO  voxelType is a org.janelia.saalfeldlab.n5.DataType
	@Getter
	@Setter
	private String voxelType;

	
	@Getter
	@Setter
	private String metadata;
	
	@Getter
	@Setter
	private long[] dimensions;
	
	@Getter
	@Setter
	private int timepoints;
	
	@Getter
	@Setter
	private int channels;
	
	@Getter
	@Setter
	private int angles;
	
	@Getter
	@Setter
	private String voxelUnit;
	
	@Getter
	@Setter
	private double[] voxelResolution;
	
	@Getter
	@Setter
	private Resolution timepointResolution;
	
	@Getter
	@Setter
	private Resolution channelResolution;
	
	@Getter
	@Setter
	private Resolution angleResolution;
	
	@Getter
	@Setter
	private String compression;

	public int[] getBlockDimension(int[] resolution) {

		for (ResolutionLevel resLev : getResolutionLevel()) {
			if (Arrays.equals(resolution, resLev.getResolutions())) {
				return resLev.getBlockDimensions();
			}
		}
		return null;
	}
}
