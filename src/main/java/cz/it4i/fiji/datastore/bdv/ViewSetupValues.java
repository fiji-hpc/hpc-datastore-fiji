/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2022 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.bdv;

import static java.lang.String.format;

import cz.it4i.fiji.datastore.core.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.sequence.Angle;
import mpicbg.spim.data.sequence.Channel;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
class ViewSetupValues {

	private final int setupId;
	private final int angleId;
	private final int channelId;
	private final String version;
	

	public static ViewSetupValues construct(int setupID,
		AbstractSequenceDescription<?, ?, ?> sequenceDescription)
	{
		BasicViewSetup bvs = sequenceDescription.getViewSetups().get(setupID);
		if (bvs == null) {
			throw new IllegalArgumentException(format(
				"ViewSetup with id {} not exists!", setupID));
		}
		return new ViewSetupValues(setupID, bvs.getAttribute(Angle.class).getId(),
			bvs.getAttribute(Channel.class).getId(), bvs.getAttribute(Version.class)
				.getName());
	}
}
