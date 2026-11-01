package se.cloudneutral.orders.client.shell.support;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.springframework.shell.jline.PromptProvider;
import org.springframework.stereotype.Component;

@Component
public class ShellPromptProvider implements PromptProvider {
    @Override
    public AttributedString getPrompt() {
        return new AttributedStringBuilder()
                .append("order-system", AttributedStyle.DEFAULT
                        .foreground(AttributedStyle.GREEN | AttributedStyle.BRIGHT))
                .append(" $ ", AttributedStyle.DEFAULT
                        .foreground(AttributedStyle.BLUE | AttributedStyle.BRIGHT))
                .toAttributedString();
    }
}

