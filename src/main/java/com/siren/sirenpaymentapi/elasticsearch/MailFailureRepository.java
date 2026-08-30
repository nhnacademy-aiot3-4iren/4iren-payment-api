package com.siren.sirenpaymentapi.elasticsearch;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface MailFailureRepository extends ElasticsearchRepository<MailFailureDocument, String> {
}
