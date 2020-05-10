SELECT id,
       source_path                                                                    AS "source",
       target_path                                                                    AS "target",
       file_size                                                                      AS "size",
       FROM_UNIXTIME(join_time / 1000, '%Y-%m-%d %H:%i:%s')                           AS "join",
       if(start_time = 0, '-', FROM_UNIXTIME(start_time / 1000, '%Y-%m-%d %H:%i:%s')) AS "start",
       if(end_time = 0, '-', FROM_UNIXTIME(end_time / 1000, '%Y-%m-%d %H:%i:%s'))     AS "end",
       if(end_time - start_time <= 0, '-', (end_time - start_time) / 1000)            AS "cost",
       convert_status                                                                 AS "status",
       retry                                                                          AS "retry",
       exceptions                                                                     AS "exceptions"
FROM `convert_info`