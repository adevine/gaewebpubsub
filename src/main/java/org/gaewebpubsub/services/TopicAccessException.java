/*
   Copyright 2012 Alexander Devine

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.gaewebpubsub.services;

/**
 * This exception is used to indicate when there is an error loading or saving information about a topic.
 */
public class TopicAccessException extends RuntimeException {
    public TopicAccessException() {
    }

    public TopicAccessException(String s) {
        super(s);
    }

    public TopicAccessException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public TopicAccessException(Throwable throwable) {
        super(throwable);
    }
}
