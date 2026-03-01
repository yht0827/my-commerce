package com.loopers.infrastructure.platform;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.loopers.interfaces.api.common.ApiResponse;

@FeignClient(
	name = "${clients.data-platform.name}",
	url = "${clients.data-platform.url}",
	path = "/data-platform/v1/events"
)
public interface DataPlatformFeignApi {

	@PostMapping
	ApiResponse<Void> send(@RequestBody DataPlatformFeignDto.EventRequest request);
}
