package se.magnus.microservices.core.recommendation.services;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.microservices.core.recommendation.persistence.RecommendationEntity;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RecommendationMapper {

	@Mappings({
		@Mapping(target = "id", ignore = true),
		@Mapping(target = "version", ignore = true),
		@Mapping(target =  "rating", source = "api.rate")
	})
	public RecommendationEntity apiToEntity(Recommendation api);
	
	@Mappings({
		@Mapping(target = "serviceAddress", ignore = true),
		@Mapping(target = "rate", source = "entity.rating")
	})
	public Recommendation entityToApi(RecommendationEntity entity);
	
	
	
	public List<RecommendationEntity> apiListToEntityList(List<Recommendation> apiList);
	
	public List<Recommendation> entityListToApiList(List<RecommendationEntity> entityList);
}
