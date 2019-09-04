package com.example.demo.model;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection="position_info_zhipin")
@Data
public class PositionInfoZhipin extends PositionInfo {
}
