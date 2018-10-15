package com.github.thinkerou.karate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.github.thinkerou.karate.constants.DescriptorFile;
import com.github.thinkerou.karate.service.GrpcList;
import com.github.thinkerou.karate.utils.Helper;
import com.google.gson.Gson;
import com.google.protobuf.DescriptorProtos;

import redis.clients.jedis.Jedis;
import redis.clients.util.SafeEncoder;

/**
 * RedisClient
 *
 * @author thinkerou
 */
public class RedisClient {

    private static final int REDIS_TIMEOUT = 3000;

    private static GrpcList listIns;
    private static Jedis jedis;

    public static RedisClient create(String host, int port) {
        listIns = GrpcList.create();
        jedis = new Jedis(host, port, REDIS_TIMEOUT);
        return new RedisClient();
    }

    public Boolean insert() {
        List<Map<String, Object>> lists = listIns.invokeForRedis();

        for (Map<String, Object> list : lists) {
            for (Map.Entry<String, Object> entry : list.entrySet()) {
                Map<String, String> hash = new HashMap<>();
                hash.put("name", entry.getKey());
                // hash.put("address", "");

                Gson gson = new Gson();
                Map<String, Object> oo = gson.fromJson(entry.getValue().toString(), Map.class);
                for (Map.Entry<String, Object> en : oo.entrySet()) {
                    if (en.getKey().equals("file")) {
                        hash.put("file", en.getValue().toString());
                        continue;
                    }
                    hash.put("input", en.getKey());
                    hash.put("message", en.getValue().toString());
                }

                jedis.hmset(entry.getKey(), hash);
            }
        }
        return true;
    }

    public Boolean insertBinaryData() {
        String path = DescriptorFile.PROTO.getText();
        Path descriptorPath = Paths.get(System.getProperty("user.dir") + path);
        Helper.validatePath(Optional.ofNullable(descriptorPath));

        byte[] data;
        try {
            data = Files.readAllBytes(descriptorPath);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        Map<String, String> hash = new HashMap<>();
        hash.put("file-descriptor-set", SafeEncoder.encode(data));

        String status = jedis.hmset("karate-grpc-protobuf", hash);
        if (status.equals("OK")) {
            return true;
        }
        return false;
    }

}