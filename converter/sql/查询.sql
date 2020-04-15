SELECT id,
       source_path                                            AS "source",
       target_path                                            AS "target",
       file_size                                              AS "size",
       FROM_UNIXTIME(join_time / 1000)                        AS "join",
       FROM_UNIXTIME(start_time / 1000)                       AS "start",
       FROM_UNIXTIME(end_time / 1000)                         AS "end",
       FROM_UNIXTIME((end_time - start_time) / 1000, "%i:%s") AS "cost",
       convert_status                                         AS "status",
       retry                                                  AS "retry",
       exceptions                                             AS "exceptions"
FROM `convert_info`