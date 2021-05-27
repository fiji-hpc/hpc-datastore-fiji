/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2021 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore;

import static io.restassured.RestAssured.with;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.config.RedirectConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.nio.ByteBuffer;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import lombok.extern.log4j.Log4j2;

/**
 * @author Jan Kožusznik
 */
@Log4j2
@QuarkusTest()
@TestInstance(Lifecycle.PER_CLASS)
public class TestDatastore {

	private String uuid;

	@BeforeEach
	void initUUID() {
		if (uuid != null) {
			return;
		}
		Response result = with().when().contentType("application/json").body(
			" { \"voxelType\":\"uint32\", \"dimensions\":[\"1000\",\"1000\",\"1\"], \"timepoints\":\"3\", \"channels\":\"3\", \"angles\":\"3\", \"voxelUnit\": \"um\", \"voxelResolution\": [\"0.4\", \"0.4\", \"1\"], \"timepointResolution\": {\"value\":\"1\",\"unit\":\"min\"}, \"channelResolution\": {\"value\":\"0\",\"unit\":null}, \"angleResolution\": {\"value\":\"0\",\"unit\":null}, \"compression\": \"raw\", \"resolutionLevels\": [ {\"resolutions\":[\"1\",\"1\",\"1\"],\"blockDimensions\":[\"64\",\"64\",\"64\"] }, {\"resolutions\":[\"2\",\"2\",\"1\"],\"blockDimensions\":[\"64\",\"64\",\"64\"]} ]}")
			.post("/datasets").andReturn();
		uuid = result.asString();
		log.info("status {}", result.getStatusLine());
	}

	@Test
	public void createDataset() {
		assertNotNull(uuid, "Dataset was not created");
	}

	@Test
	public void writeReadOneBlock() {
		Response result = withNoFollowRedirects().get("/datasets/" + uuid +
			"/1/1/1/new-for-writing-only?timeout=10000");
		assertEquals(307, result.getStatusCode(), "Should be redirected");

		String redirectedURI = result.getHeader("Location");
		
		byte[] data = constructOneBlock(64);

		with().baseUri(redirectedURI).contentType(ContentType.BINARY).body(data)
			.post("/0/0/0/0/0/0");
		with().baseUri(redirectedURI).post("/stop");

		result = with().config(RestAssuredConfig.config().redirect(RedirectConfig
			.redirectConfig().followRedirects(false))).get("/datasets/" + uuid +
				"/1/1/1/latest-for-reading-only?timeout=10000");
		assertEquals(307, result.getStatusCode(), "Should be redirected");
		redirectedURI = result.getHeader("Location");
		result = with().baseUri(redirectedURI).contentType(	ContentType.BINARY).get("/0/0/0/0/0/0");
		with().baseUri(redirectedURI).post("/stop");
		assertEquals(ContentType.BINARY.toString(), result.contentType());
		byte[] outputData = result.getBody().asByteArray();
		assertArrayEquals(data, outputData);
	}

	@Test
	public void writeReadTwoBlocks() {
		Response result = withNoFollowRedirects().get("/datasets/" + uuid +
			"/1/1/1/new-for-writing-only?timeout=10000");
		assertEquals(307, result.getStatusCode(), "Should be redirected");

		String redirectedURI = result.getHeader("Location");

		byte[] data = constructBlocks(2, 64);

		with().baseUri(redirectedURI).contentType(ContentType.BINARY).body(data)
			.post("/0/0/0/0/0/0/0/1/0/0/0/0");
		with().baseUri(redirectedURI).post("/stop");

		result = with().config(RestAssuredConfig.config().redirect(RedirectConfig
			.redirectConfig().followRedirects(false))).get("/datasets/" + uuid +
				"/1/1/1/latest-for-reading-only?timeout=10000");
		assertEquals(307, result.getStatusCode(), "Should be redirected");
		redirectedURI = result.getHeader("Location");
		result = with().baseUri(redirectedURI).contentType(ContentType.BINARY).get(
			"/0/0/0/0/0/0/0/1/0/0/0/0");
		with().baseUri(redirectedURI).post("/stop");
		assertEquals(ContentType.BINARY.toString(), result.contentType(),
			"expected binary but obtained: " + result.body().asString());
		byte[] outputData = result.getBody().asByteArray();
		assertArrayEquals(data, outputData);


	}

	@Test
	public void mixedLatest() {
		String baseURI = withNoFollowRedirects().get("/datasets/" + uuid +
			"/1/1/1/new-for-writing-only?timeout=10000").getHeader("Location");
		byte[] block1 = constructBlocks(1, 64);
		with().baseUri(baseURI).contentType(ContentType.BINARY).body(block1).post(
			"/0/0/0/0/0/0");
		with().baseUri(baseURI).post("/stop");

		baseURI = withNoFollowRedirects().get("/datasets/" + uuid +
			"/1/1/1/new-for-writing-only?timeout=10000").getHeader("Location");
		byte[] block2 = constructBlocks(1, 64);

		with().baseUri(baseURI).contentType(ContentType.BINARY).body(block2).post(
			"/0/1/0/0/0/0");
		with().baseUri(baseURI).post("/stop");

		baseURI = withNoFollowRedirects().get("/datasets/" + uuid +
			"/1/1/1/mixedLatest-for-reading-only?timeout=10000").getHeader(
				"Location");

		byte[] sentData = new byte[block1.length + block2.length];
		System.arraycopy(block1, 0, sentData, 0, block1.length);
		System.arraycopy(block2, 0, sentData, block1.length, block2.length);

		Response result = with().baseUri(baseURI).contentType(ContentType.BINARY)
			.get("/0/0/0/0/0/0/0/1/0/0/0/0");
		with().baseUri(baseURI).post("/stop");
		assertEquals(ContentType.BINARY.toString(), result.contentType(),
			"expected binary but obtained: " + result.body().asString());
		byte[] outputData = result.getBody().asByteArray();
		assertArrayEquals(sentData, outputData);
	}

	private RequestSpecification withNoFollowRedirects() {
		return with().config(RestAssuredConfig.config().redirect(RedirectConfig
			.redirectConfig().followRedirects(false)));
	}

	private byte[] constructOneBlock(int dim) {
		return constructBlocks(1, dim);
	}

	private byte[] constructBlocks(int num, int dim) {
		ByteBuffer bb = ByteBuffer.allocate(4);
		bb.putInt(dim);
		int sizeOfOneBlock = (dim * dim * dim + 3) * 4;
		byte[] data = new byte[sizeOfOneBlock * num];
		new Random().nextBytes(data);
		for (int i = 0; i < num; i++) {
			int offset = sizeOfOneBlock * i;
			bb.flip();
			bb.get(data, offset + 0, 4);
			bb.clear();
			bb.get(data, offset + 4, 4);
			bb.clear();
			bb.get(data, offset + 8, 4);
		}
		return data;
	}

}
