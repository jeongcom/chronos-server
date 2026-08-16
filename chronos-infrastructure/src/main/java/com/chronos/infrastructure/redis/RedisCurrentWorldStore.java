package com.chronos.infrastructure.redis;

import com.chronos.application.port.CurrentWorldStore;
import com.chronos.domain.world.WorldState;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public class RedisCurrentWorldStore implements CurrentWorldStore {
    private final StringRedisTemplate redis; private final JsonMapper json;
    public RedisCurrentWorldStore(StringRedisTemplate redis,JsonMapper json){this.redis=redis;this.json=json;}
    private String key(String spaceId){return "chronos:world:"+spaceId;}
    @Override public Optional<WorldState> find(String spaceId){
        String value=redis.opsForValue().get(key(spaceId));
        if(value==null)return Optional.empty();
        try{return Optional.of(json.readValue(value,WorldState.class));}catch(Exception e){throw new IllegalStateException(e);}
    }
    @Override public void save(WorldState state){
        try{redis.opsForValue().set(key(state.spaceId()),json.writeValueAsString(state));}catch(Exception e){throw new IllegalStateException(e);}
    }
}
