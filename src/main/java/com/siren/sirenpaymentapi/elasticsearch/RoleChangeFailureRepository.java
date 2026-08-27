package com.siren.sirenpaymentapi.elasticsearch;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface RoleChangeFailureRepository extends ElasticsearchRepository<RoleChangeFailureDocument, String> {
}
