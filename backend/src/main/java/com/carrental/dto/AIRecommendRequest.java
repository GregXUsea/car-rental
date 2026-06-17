package com.carrental.dto;

import lombok.Data;

@Data
public class AIRecommendRequest {
    /** 用户自然语言需求描述 */
    private String requirement;
    /** 多轮对话上下文ID，首次请求为空，后续追问携带上次返回的conversationId */
    private String conversationId;
    /** 换一批刷新标记，为true时要求AI给出与上次不同的推荐 */
    private Boolean refresh;
}
