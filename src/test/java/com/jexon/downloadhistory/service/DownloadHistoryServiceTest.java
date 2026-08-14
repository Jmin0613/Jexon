package com.jexon.downloadhistory.service;

import com.jexon.downloadhistory.domain.DownloadHistory;
import com.jexon.downloadhistory.repository.DownloadHistoryRepository;
import com.jexon.gamefile.domain.GameFile;
import com.jexon.gameversion.domain.GameVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DownloadHistoryServiceTest {
    @Mock DownloadHistoryRepository downloadHistoryRepository;
    @Mock GameVersion gameVersion;
    @Mock GameFile gameFile;
    @InjectMocks DownloadHistoryService service;

    @Test
    void recordsOneDownloadHistory() {
        service.record(gameVersion, gameFile);

        ArgumentCaptor<DownloadHistory> captor = ArgumentCaptor.forClass(DownloadHistory.class);
        verify(downloadHistoryRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getGameVersion()).isSameAs(gameVersion);
        assertThat(captor.getValue().getGameFile()).isSameAs(gameFile);
    }
}
