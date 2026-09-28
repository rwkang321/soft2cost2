package com.soft2cost2.common.i18n;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 기존 메뉴 저장 트랜잭션에서 ko 이름만 동기화하며, 다른 번역과 권한은 보존 한다. */
@Service("menuTranslationWriter")
public class MenuTranslationWriterImpl implements MenuTranslationWriter {
    private final I18nMapper mapper;
    public MenuTranslationWriterImpl(I18nMapper mapper) { this.mapper = mapper; }

    @Transactional
    @Override
    public void syncDefault(Long menuId, String actor) {
        if (mapper.syncDefaultMenu(menuId, actor) != 1) {
            throw new IllegalStateException("Default menu translation synchronization failed!!!");
        }
    }

}
