package cn.bugstack.recite.domain.progress;

import cn.bugstack.recite.domain.progress.model.entity.UserStreakEntity;
import cn.bugstack.recite.domain.progress.port.out.StreakPort;
import cn.bugstack.recite.domain.progress.service.StreakService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("连续天数")
@ExtendWith(MockitoExtension.class)
class StreakServiceTest {

    @Mock
    private StreakPort streakPort;

    private StreakService service;

    @BeforeEach
    void setUp() {
        service = new StreakService(streakPort);
    }

    @Nested
    @DisplayName("首次签到")
    class FirstCheckIn {

        @Test
        @DisplayName("无历史记录 → streak=1 longest=1 last=today")
        void shouldCreateInitialStreak() {
            when(streakPort.findByUserId(1L)).thenReturn(Optional.empty());

            ArgumentCaptor<UserStreakEntity> captor = ArgumentCaptor.forClass(UserStreakEntity.class);
            service.checkIn(1L);

            verify(streakPort).save(captor.capture());
            UserStreakEntity saved = captor.getValue();

            assertThat(saved.getUserId()).isEqualTo(1L);
            assertThat(saved.getCurrentStreak()).isEqualTo(1);
            assertThat(saved.getLongestStreak()).isEqualTo(1);
            assertThat(saved.getLastActiveDate()).isEqualTo(LocalDate.now());
        }
    }

    @Nested
    @DisplayName("连续签到")
    class ConsecutiveCheckIn {

        @Test
        @DisplayName("昨天背过 → streak+1 longest 更新")
        void shouldIncrementStreak() {
            var existing = UserStreakEntity.builder()
                    .userId(1L).currentStreak(3).lastActiveDate(LocalDate.now().minusDays(1))
                    .longestStreak(3).build();
            when(streakPort.findByUserId(1L)).thenReturn(Optional.of(existing));

            ArgumentCaptor<UserStreakEntity> captor = ArgumentCaptor.forClass(UserStreakEntity.class);
            service.checkIn(1L);

            verify(streakPort).update(captor.capture());
            UserStreakEntity updated = captor.getValue();

            assertThat(updated.getCurrentStreak()).isEqualTo(4);
            assertThat(updated.getLastActiveDate()).isEqualTo(LocalDate.now());
            assertThat(updated.getLongestStreak()).isEqualTo(4);
        }

        @Test
        @DisplayName("今天已背过 → streak 不变")
        void sameDayShouldNotChange() {
            var existing = UserStreakEntity.builder()
                    .userId(1L).currentStreak(5).lastActiveDate(LocalDate.now())
                    .longestStreak(5).build();
            when(streakPort.findByUserId(1L)).thenReturn(Optional.of(existing));

            ArgumentCaptor<UserStreakEntity> captor = ArgumentCaptor.forClass(UserStreakEntity.class);
            service.checkIn(1L);

            verify(streakPort).update(captor.capture());
            UserStreakEntity updated = captor.getValue();

            assertThat(updated.getCurrentStreak()).isEqualTo(5);     // 不变
            assertThat(updated.getLongestStreak()).isEqualTo(5);     // 不变
        }
    }

    @Nested
    @DisplayName("断签")
    class StreakBreak {

        @Test
        @DisplayName("前天背过但昨天没背 → reset to 1")
        void shouldResetAfterGap() {
            var existing = UserStreakEntity.builder()
                    .userId(1L).currentStreak(10).lastActiveDate(LocalDate.now().minusDays(2))
                    .longestStreak(15).build();
            when(streakPort.findByUserId(1L)).thenReturn(Optional.of(existing));

            ArgumentCaptor<UserStreakEntity> captor = ArgumentCaptor.forClass(UserStreakEntity.class);
            service.checkIn(1L);

            verify(streakPort).update(captor.capture());
            UserStreakEntity updated = captor.getValue();

            assertThat(updated.getCurrentStreak()).isEqualTo(1);     // 重置
            assertThat(updated.getLongestStreak()).isEqualTo(15);    // 保持历史最长
        }

        @Test
        @DisplayName("last=null（异常数据）→ reset to 1")
        void nullLastDateShouldReset() {
            var existing = UserStreakEntity.builder()
                    .userId(1L).currentStreak(7).lastActiveDate(null)
                    .longestStreak(10).build();
            when(streakPort.findByUserId(1L)).thenReturn(Optional.of(existing));

            ArgumentCaptor<UserStreakEntity> captor = ArgumentCaptor.forClass(UserStreakEntity.class);
            service.checkIn(1L);

            verify(streakPort).update(captor.capture());
            assertThat(captor.getValue().getCurrentStreak()).isEqualTo(1);
        }

        @Test
        @DisplayName("断签后再恢复 → 新 streak 从 1 开始")
        void shouldStartNewStreakAfterBreak() {
            var day1 = UserStreakEntity.builder()
                    .userId(1L).currentStreak(1).lastActiveDate(LocalDate.now().minusDays(2))
                    .longestStreak(5).build();
            when(streakPort.findByUserId(1L)).thenReturn(Optional.of(day1));

            var afterBreak = service.checkIn(1L);
            assertThat(afterBreak.getCurrentStreak()).isEqualTo(1);

            // 第二天再签
            var afterBreakDay2 = UserStreakEntity.builder()
                    .userId(1L).currentStreak(1).lastActiveDate(LocalDate.now())
                    .longestStreak(5).build();
            when(streakPort.findByUserId(1L)).thenReturn(Optional.of(afterBreakDay2));

            // 但 afterBreakDay2.lastActiveDate = today, next checkIn tomorrow would be:
            var day2 = UserStreakEntity.builder()
                    .userId(1L).currentStreak(1).lastActiveDate(LocalDate.now().minusDays(1))
                    .longestStreak(5).build();
            when(streakPort.findByUserId(1L)).thenReturn(Optional.of(day2));

            var r = service.checkIn(1L);
            assertThat(r.getCurrentStreak()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("最长记录")
    class LongestStreak {

        @Test
        @DisplayName("新 streak 超过历史最长 → 更新 longest")
        void shouldUpdateLongestWhenNewStreakExceeds() {
            var existing = UserStreakEntity.builder()
                    .userId(1L).currentStreak(10).lastActiveDate(LocalDate.now().minusDays(1))
                    .longestStreak(10).build();
            when(streakPort.findByUserId(1L)).thenReturn(Optional.of(existing));

            var result = service.checkIn(1L);

            assertThat(result.getCurrentStreak()).isEqualTo(11);
            assertThat(result.getLongestStreak()).isEqualTo(11);
        }

        @Test
        @DisplayName("断签后 longest 保持不变")
        void longestShouldPersistThroughBreak() {
            var existing = UserStreakEntity.builder()
                    .userId(1L).currentStreak(7).lastActiveDate(LocalDate.now().minusDays(3))
                    .longestStreak(30).build();
            when(streakPort.findByUserId(1L)).thenReturn(Optional.of(existing));

            var result = service.checkIn(1L);

            assertThat(result.getCurrentStreak()).isEqualTo(1);
            assertThat(result.getLongestStreak()).isEqualTo(30);
        }
    }
}
