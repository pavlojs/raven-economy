/*
 * Copyright 2026 pavlojs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.whiteravens.ravencoin.client;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.whiteravens.ravencoin.network.AtmListPayload;
import net.whiteravens.ravencoin.network.AtmNoticePayload;

/**
 * The far end of the two packets that travel towards a client.
 *
 * <p>This class exists so that common code can name a handler without naming a
 * screen. A dedicated server loads this class when it registers the channels —
 * which it must, because it is the side that sends them — but a screen is only
 * ever reached from inside these method bodies, and a body is not resolved
 * until it runs. On a server it never runs.
 */
public final class ClientPayloads {
    public static void list(AtmListPayload payload, IPayloadContext context) {
        AtmScreen.accept(payload, context);
    }

    public static void notice(AtmNoticePayload payload, IPayloadContext context) {
        AtmScreen.notice(payload, context);
    }

    private ClientPayloads() {}
}
