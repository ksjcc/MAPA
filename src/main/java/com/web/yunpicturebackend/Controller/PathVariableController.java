/*
 * Copyright 2013-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.web.yunpicturebackend.Controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author <a href="mailto:chenxilzx1@gmail.com">theonefx</a>
 */
@Controller
@Tag(name = "路径参数示例", description = "演示路径参数与正则匹配")
public class PathVariableController {

    // http://127.0.0.1:8080/user/123/roles/222
    @RequestMapping(value = "/user/{userId}/roles/{roleId}", method = RequestMethod.GET)
    @ResponseBody
    @Operation(summary = "根据用户与角色ID返回拼接信息", description = "演示多路径参数处理")
    public String getLogin(@Parameter(description = "用户ID", required = true) @PathVariable("userId") String userId,
            @Parameter(description = "角色ID", required = true) @PathVariable("roleId") String roleId) {
        return "User Id : " + userId + " Role Id : " + roleId;
    }

    // http://127.0.0.1:8080/javabeat/somewords
    @RequestMapping(value = "/javabeat/{regexp1:[a-z-]+}", method = RequestMethod.GET)
    @ResponseBody
    @Operation(summary = "正则路径匹配示例", description = "演示路径中使用正则进行匹配")
    public String getRegExp(
            @Parameter(description = "匹配的正则表达式部分", required = true) @PathVariable("regexp1") String regexp1) {
        return "URI Part : " + regexp1;
    }
}
