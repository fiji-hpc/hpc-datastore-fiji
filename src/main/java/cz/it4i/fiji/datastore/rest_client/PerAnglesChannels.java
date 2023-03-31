/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2023 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.rest_client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mpicbg.spim.data.generic.base.Entity;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.sequence.Angle;
import mpicbg.spim.data.sequence.Channel;


@AllArgsConstructor
class PerAnglesChannels {

	@AllArgsConstructor
	static class AngleChannel {

		private final Angle angle;
		private final Channel channel;
		private final Map<Angle, Integer> perAngleIndex;
		private final Map<Channel, Integer> perChannelIndex;

		public int getAngleIndex() {
			return perAngleIndex.get(angle);
		}

		public int getChannelIndex() {
			return perChannelIndex.get(channel);
		}
	}

	private static final Channel DEFAULT_CHANNEL = new Channel(-1);
	private static final Angle DEFAULT_ANGLE = new Angle(-1);

	@Getter
	private final int channels;

	@Getter
	private final int angles;

	private final Map<Integer, AngleChannel> perViewAngleChannels;

	static PerAnglesChannels construct(
		AbstractSequenceDescription<?, ?, ?> seq)
	{

		Map<Angle, Integer> perAngleIndex = new HashMap<>();

		Map<Channel, Integer> perChannelIndex = new HashMap<>();

		Map<Integer, AngleChannel> perViewAngleChannels = new HashMap<>();

		for (BasicViewSetup bvs : seq.getViewSetupsOrdered()) {
			Channel channel = getAttribute(bvs, Channel.class, DEFAULT_CHANNEL);
			perChannelIndex.put(channel, null);

			Angle angle = getAttribute(bvs, Angle.class, DEFAULT_ANGLE);
			perAngleIndex.put(angle, null);
			perViewAngleChannels.put(bvs.getId(), new AngleChannel(angle, channel,
				perAngleIndex, perChannelIndex));

		}
		reindex(perChannelIndex);
		reindex(perAngleIndex);


		return new PerAnglesChannels(perChannelIndex.size(), perAngleIndex.size(),
			perViewAngleChannels);
	}

	public AngleChannel getAngleChannel(int viewSetupID) {
		return perViewAngleChannels.get(viewSetupID);
	}

	private static <T extends Entity> void reindex(
		Map<T, Integer> perEntityIndex)
	{
		List<T> sortedEntity = perEntityIndex.keySet().stream().sorted().collect(
			Collectors.toList());

		int i = 0;
		for (T entity : sortedEntity) {
			perEntityIndex.replace(entity, i);
			i++;
		}

	}

	private static <T extends Entity> T getAttribute(BasicViewSetup bvs,
		Class<T> clazz, T defaultValue)
	{
		T result = bvs.getAttribute(clazz);
		if (result == null) {
			result = defaultValue;
		}
		return result;
	}

}
