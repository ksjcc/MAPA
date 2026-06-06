package com.web.yunpicturebackend.manager.sharding;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import com.web.yunpicturebackend.Service.SpaceService;
import com.web.yunpicturebackend.model.entity.Space;
import com.web.yunpicturebackend.model.enums.SpaceTypeEnum;

import lombok.extern.slf4j.Slf4j;
import javax.sql.DataSource;
import java.util.Optional;

import com.baomidou.mybatisplus.extension.toolkit.SqlRunner;
import org.apache.shardingsphere.driver.jdbc.core.connection.ShardingSphereConnection;
import org.apache.shardingsphere.infra.metadata.database.rule.ShardingSphereRuleMetaData;
import org.apache.shardingsphere.mode.manager.ContextManager;
import org.apache.shardingsphere.sharding.api.config.ShardingRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.rule.ShardingTableRuleConfiguration;
import org.apache.shardingsphere.sharding.rule.ShardingRule;

//@Component
@Slf4j
public class DynamicShardingManager {

  @Resource
  private DataSource dataSource;

  @Resource
  private SpaceService spaceService;

  private static final String LOGIC_TABLE_NAME = "picture";

  private static final String DATABASE_NAME = "yunpicture"; // 配置文件中的数据库名称

  @PostConstruct
  public void initialize() {
    log.info("初始化动态分表配置...");
    updateShardingTableNodes();
  }

  private Set<String> fetchAllPictureTableNames() {
    Set<Long> spaceIds = spaceService.lambdaQuery()
        .eq(Space::getSpaceType, SpaceTypeEnum.TEAM.getValue())
        .list()
        .stream()
        .map(Space::getId)
        .collect(Collectors.toSet());
    Set<String> tableNames = spaceIds.stream()
        .map(spaceId -> LOGIC_TABLE_NAME + "_" + spaceId)
        .collect(Collectors.toSet());
    tableNames.add(LOGIC_TABLE_NAME);
    return tableNames;
  }

  private void updateShardingTableNodes() {
    Set<String> tableNames = fetchAllPictureTableNames();
    String newActualDataNodes = tableNames.stream()
        .map(tableName -> "yu_picture." + tableName) // 确保前缀合法
        .collect(Collectors.joining(","));
    log.info("动态分表 actual-data-nodes 配置: {}", newActualDataNodes);
    ContextManager contextManager = getContextManager();
    ShardingSphereRuleMetaData ruleMetaData = contextManager.getMetaDataContexts()
        .getMetaData()
        .getDatabases()
        .get(DATABASE_NAME)
        .getRuleMetaData();
    Optional<ShardingRule> shardingRule = ruleMetaData.findSingleRule(ShardingRule.class);
    if (shardingRule.isPresent()) {
      ShardingRuleConfiguration ruleConfig = (ShardingRuleConfiguration) shardingRule.get().getConfiguration();
      List<ShardingTableRuleConfiguration> updatedRules = ruleConfig.getTables()
          .stream()
          .map(oldTableRule -> {
            if (LOGIC_TABLE_NAME.equals(oldTableRule.getLogicTable())) {
              ShardingTableRuleConfiguration newTableRuleConfig = new ShardingTableRuleConfiguration(LOGIC_TABLE_NAME,
                  newActualDataNodes);
              newTableRuleConfig.setDatabaseShardingStrategy(oldTableRule.getDatabaseShardingStrategy());
              newTableRuleConfig.setTableShardingStrategy(oldTableRule.getTableShardingStrategy());
              newTableRuleConfig.setKeyGenerateStrategy(oldTableRule.getKeyGenerateStrategy());
              newTableRuleConfig.setAuditStrategy(oldTableRule.getAuditStrategy());
              return newTableRuleConfig;
            }
            return oldTableRule;
          })
          .collect(Collectors.toList());
      ruleConfig.setTables(updatedRules);
      contextManager.alterRuleConfiguration(DATABASE_NAME, Collections.singleton(ruleConfig));
      contextManager.reloadDatabase(DATABASE_NAME);
      log.info("动态分表规则更新成功！");
    } else {
      log.error("未找到 ShardingSphere 的分片规则配置，动态分表更新失败。");
    }
  }

  private void createSpacePictureTable(Space space) {
    if (space.getSpaceType() == SpaceTypeEnum.TEAM.getValue()) {
      Long spaceId = space.getId();
      String tableName = LOGIC_TABLE_NAME + "_" + spaceId;
      String createTableSQL = String.format(
          "CREATE TABLE IF NOT EXISTS %s (LIKE %s INCLUDING ALL)",
          tableName, LOGIC_TABLE_NAME);
      try {
        SqlRunner.db().update(createTableSQL);
        updateShardingTableNodes();
        log.info("成功创建分表: {}", tableName);
      } catch (Exception e) {
        e.printStackTrace();
        log.error("创建分表 {} 失败", tableName, e);
      }
    }
  }

  /**
   * 获取 ShardingSphere ContextManager
   */
  private ContextManager getContextManager() {
    try (ShardingSphereConnection connection = dataSource.getConnection().unwrap(ShardingSphereConnection.class)) {
      return connection.getContextManager();
    } catch (SQLException e) {
      throw new RuntimeException("获取 ShardingSphere ContextManager 失败", e);
    }
  }
}
