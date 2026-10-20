package com.hollingsworth.arsnouveau.common.compat.ae2;

import appeng.api.integrations.igtooltip.BaseClassRegistration;
import appeng.api.integrations.igtooltip.TooltipProvider;

@SuppressWarnings("UnstableApiUsage")
public class SourceTooltipProvider implements TooltipProvider {
    @Override
    public void registerBlockEntityBaseClasses(BaseClassRegistration registration) {
        registration.addBaseBlockEntity(MESourceJarBlockEntity.class, MESourceJarBlock.class);
    }
}
