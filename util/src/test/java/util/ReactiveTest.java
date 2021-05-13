package util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import reactor.core.publisher.Flux;

public class ReactiveTest {

	@Test
	public void TestFlux() {
		List<Integer> list = new ArrayList<>();
		
		Flux.just(1, 2, 3, 4).filter(n -> n % 2 == 0).map(n -> n * 2).log().subscribe(n->list.add(n));
		
		assertThat(list).containsExactly(4,8);
	}
	
}
