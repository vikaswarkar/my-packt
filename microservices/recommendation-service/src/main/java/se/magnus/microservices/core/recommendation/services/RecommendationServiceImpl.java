package se.magnus.microservices.core.recommendation.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.RestController;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.api.core.recommendation.RecommendationService;
import se.magnus.microservices.core.recommendation.persistence.RecommendationEntity;
import se.magnus.microservices.core.recommendation.persistence.RecommendationRepository;
import se.magnus.util.exceptions.InvalidInputException;
import se.magnus.util.http.ServiceUtil;

import java.util.List;

@Slf4j
@RestController
public class RecommendationServiceImpl implements RecommendationService {


    private final ServiceUtil serviceUtil;
	private RecommendationRepository repository;
	private RecommendationMapper mapper;

    @Autowired
    public RecommendationServiceImpl(ServiceUtil serviceUtil, RecommendationMapper mapper, RecommendationRepository repository) {
        this.serviceUtil = serviceUtil;
        this.repository = repository;
        this.mapper = mapper;
        
    }

    @Override
    public List<Recommendation> getRecommendations(int productId) {

        if (productId < 1) throw new InvalidInputException("Invalid productId: " + productId);

        List<RecommendationEntity> recommendationEntityList = repository.findByProductId(productId);
        List<Recommendation> recommendationList = mapper.entityListToApiList(recommendationEntityList);	

        log.debug("getRecommendation response size: {}", recommendationList.size());
        recommendationList.forEach(r -> r.setServiceAddress(serviceUtil.getServiceAddress()));
        return recommendationList;
    }

	@Override
	public Recommendation createRecommendation(Recommendation recommendation) {
		try {
			RecommendationEntity entity = mapper.apiToEntity(recommendation);
			entity = repository.save(entity);			
			log.debug("Recommendation {} successfully saved ", entity);
			return mapper.entityToApi(entity);
		}catch(DataIntegrityViolationException deve) {
			log.error("Error saving recommendation - {} ", recommendation);
			log.error("Error Message - {} ", deve.getMessage());
			throw new InvalidInputException("Error saving recommendation - {} \", recommendation");
		}
		
	}

	@Override
	public void deleteRecommendations(int productId) {
		log.debug("Deleting all recommendations for Product {}", productId);
		repository.deleteAll(repository.findByProductId(productId));
	}
}
