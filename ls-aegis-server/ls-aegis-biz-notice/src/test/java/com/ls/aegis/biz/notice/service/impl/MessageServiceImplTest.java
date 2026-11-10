package com.ls.aegis.biz.notice.service.impl;

import cn.dev33.satoken.exception.NotPermissionException;
import com.ls.aegis.common.context.UserContext;
import com.ls.aegis.common.context.UserContextHolder;
import com.ls.aegis.biz.notice.mapper.MessageMapper;
import com.ls.aegis.biz.notice.service.MessageLogService;
import com.ls.aegis.biz.notice.model.query.MessageQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MessageServiceImplTest {
    private final MessageMapper mapper = mock(MessageMapper.class);
    private final MessageLogService logs = mock(MessageLogService.class);
    private final MessageServiceImpl service = new MessageServiceImpl(mapper, logs);

    private void user(Long id) {
        UserContext context = new UserContext();
        context.setId(id);
        UserContextHolder.setContext(context, false);
    }

    @AfterEach
    void clearContext() { UserContextHolder.clearContext(); }

    @Test
    void foreignObjectCannotBeDeletedEvenIfTheIdIsKnown() {
        user(202L);
        when(mapper.selectAccessibleIds(List.of(1L), 202L)).thenReturn(List.of());
        assertThrows(NotPermissionException.class, () -> service.delete(List.of(1L)));
        verify(mapper).selectAccessibleIds(List.of(1L), 202L);
        verifyNoMoreInteractions(mapper);
        verifyNoInteractions(logs);
    }

    @Test
    void mixedBatchIsRejectedBeforeAnyWrite() {
        user(101L);
        when(mapper.selectAccessibleIds(List.of(1L, 2L), 101L)).thenReturn(List.of(1L));
        assertThrows(NotPermissionException.class, () -> service.delete(List.of(1L, 2L)));
        verifyNoInteractions(logs);
        verify(mapper).selectAccessibleIds(List.of(1L, 2L), 101L);
        verifyNoMoreInteractions(mapper);
    }

    @Test
    void sharedAndBroadcastRecipientsOnlyWriteTheirOwnTombstones() {
        when(mapper.selectAccessibleIds(List.of(1L, 2L), 101L)).thenReturn(List.of(1L, 2L));
        when(mapper.selectAccessibleIds(List.of(1L, 2L), 202L)).thenReturn(List.of(1L, 2L));
        user(101L); service.delete(List.of(1L, 2L));
        user(202L); service.delete(List.of(1L, 2L));
        verify(logs).markDeletedForUser(List.of(1L, 2L), 101L);
        verify(logs).markDeletedForUser(List.of(1L, 2L), 202L);
        verify(mapper).selectAccessibleIds(List.of(1L, 2L), 101L);
        verify(mapper).selectAccessibleIds(List.of(1L, 2L), 202L);
        verifyNoMoreInteractions(mapper, logs);
    }

    @Test
    void repeatedDeleteDeduplicatesInputAndDoesNotGloballyDeleteMessage() {
        user(101L);
        when(mapper.selectAccessibleIds(List.of(1L), 101L)).thenReturn(List.of(1L));
        service.delete(List.of(1L, 1L));
        service.delete(List.of(1L));
        verify(logs, times(2)).markDeletedForUser(List.of(1L), 101L);
        verify(mapper, times(2)).selectAccessibleIds(List.of(1L), 101L);
        verifyNoMoreInteractions(mapper, logs);
    }

    @Test
    void crossedBatchesUseTheSameSortedLockOrder() {
        user(101L);
        when(mapper.selectAccessibleIds(List.of(1L, 2L), 101L)).thenReturn(List.of(1L, 2L));
        service.delete(List.of(2L, 1L, 2L));
        service.delete(List.of(1L, 2L));
        verify(logs, times(2)).markDeletedForUser(List.of(1L, 2L), 101L);
    }

    @Test
    void pageAndDetailCannotTrustCallerSuppliedRecipient() {
        user(101L);
        MessageQuery query = new MessageQuery(); query.setUserId(999L);
        when(mapper.selectMessagePage(any(), same(query))).thenReturn(new Page<>());
        service.page(query, new top.continew.starter.extension.crud.model.query.PageQuery());
        assertEquals(101L, query.getUserId());
        assertNull(service.get(1L));
        verify(mapper).selectMessageById(1L, 101L);
    }

    @Test
    void missingIdentityCannotReadOrWriteInbox() {
        user(null);
        assertThrows(NotPermissionException.class, () -> service.delete(List.of(1L)));
        assertThrows(NotPermissionException.class, () -> service.get(1L));
        verifyNoInteractions(mapper, logs);
    }

    @Test
    void groupedUnreadUsesOneQueryAndFillsMissingTypesWithZero() {
        var system=new com.ls.aegis.biz.notice.model.resp.message.MessageTypeCount();
        system.setType(1);system.setCount(123L);
        var unknown=new com.ls.aegis.biz.notice.model.resp.message.MessageTypeCount();
        unknown.setType(99);unknown.setCount(1000L);
        when(mapper.selectUnreadCountsByType(101L)).thenReturn(List.of(system,unknown));
        var result=service.countUnreadByUserId(101L,true);
        assertEquals(123L,result.getTotal());assertEquals(2,result.getDetails().size());
        assertEquals(0L,result.getDetails().get(1).getCount());
        verify(mapper).selectUnreadCountsByType(101L);verifyNoMoreInteractions(mapper);
    }

    @Test
    void totalOnlyRemainsSingleCountAndDoesNotLoadMessages() {
        when(mapper.selectUnreadCountByUserIdAndType(202L,null)).thenReturn(345L);
        assertEquals(345L,service.countUnreadByUserId(202L,false).getTotal());
        verify(mapper).selectUnreadCountByUserIdAndType(202L,null);verifyNoMoreInteractions(mapper);
    }

}
